/*
 * Copyright (C) 2012 Roman Elizarov
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.avrbuddy.xbee.discover;

import org.avrbuddy.hex.HexUtil;
import org.avrbuddy.log.Log;
import org.avrbuddy.xbee.api.*;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Roman Elizarov
 */
public class XBeeNodeDiscovery {
    private static final Logger log = Log.getLogger(XBeeNodeDiscovery.class);

    public static final int DISCOVER_ATTEMPTS = 3;

    private static final int MIN_DISCOVERY_TIMEOUT = 0x20;
    private static final int MAX_DISCOVERY_TIMEOUT = 0xff;
    private static final long DISCOVERY_TIMEOUT_UNIT = 100L;

    private final XBeeConnection conn;

    private XBeeNode localNode;
    private final Map<String, XBeeNode> nodeById = new HashMap<String, XBeeNode>();
    private final Map<XBeeAddress, XBeeNode> nodeByAddress = new HashMap<XBeeAddress, XBeeNode>();

    public XBeeNodeDiscovery(XBeeConnection conn) {
        this.conn = conn;
    }

    public void discoverAllNodes(XBeeNodeVisitor visitor) throws IOException {
        int status = Math.max(
                discoverDestinationNode(null, visitor),
                discoverRemoteNode(null, visitor, 0));
        if (status != XBeeAtResponseFrame.STATUS_OK)
            throw new XBeeException(XBeeUtil.formatStatus(status));
    }

    // never returns null (will throw IOException)
    public XBeeNode getOrDiscoverLocalNode() throws IOException {
        XBeeNode node = getLocalNode();
        if (node != null) {
            log.info("Using local address " + node);
            return node;
        }
        log.info("Retrieving local node address");
        try {
            node = checkStatus(discoverDestinationNode(null, null), getLocalNode());
        } catch (IOException e) {
            throw new IOException("Failed to discover local node: " + e.getMessage(), e);
        }
        log.info("Retrieved local node " + node);
        return node;
    }

    public XBeeNode getOrDiscoverByNodeId(String id, int attempts) throws IOException {
        if (id == null)
            throw new NullPointerException();
        XBeeNode node = getByNodeId(id);
        if (node != null) {
            log.info("Using remote address " + node);
            return node;
        }
        log.info("Discovering remote node " + XBeeNode.NODE_ID_PREFIX + id);
        int status = -1;
        for (int attempt = 0; attempt < attempts; attempt++) {
            status = discoverRemoteNode(id, null, 0);
            if (status == XBeeAtResponseFrame.STATUS_OK)
                break;
        }
        try {
            node = checkStatus(status, getByNodeId(id));
        } catch (IOException e) {
            throw new IOException("Failed to discover node " + XBeeNode.NODE_ID_PREFIX + id +
                    " after " + XBeeNodeDiscovery.DISCOVER_ATTEMPTS + " attempts: " + e.getMessage(), e);
        }
        log.info("Discovered remote node " + node);
        return node;
    }

    public XBeeNode getOrDiscoverNodeByAddress(XBeeAddress address) throws IOException {
        XBeeNode node = getNodeByAddress(address);
        if (node != null)
            return node;
        try {
            return checkStatus(discoverDestinationNode(address, null), getNodeByAddress(address));
        } catch (IOException e) {
            throw new IOException("Failed to discover node " + address + ": " + e.getMessage(), e);
        }
    }

    public synchronized XBeeNode getNodeByAddress(XBeeAddress address) {
        return nodeByAddress.get(address);
    }

    public void list(XBeeNodeVisitor visitor) {
        ArrayList<XBeeNode> nodes;
        synchronized (this) {
            nodes = new ArrayList<XBeeNode>(nodeById.values());
        }
        Collections.sort(nodes);
        for (XBeeNode node : nodes)
            visitor.visitNode(node);
    }

    // -------------- PRIVATE HELPER METHODS --------------

    // destination == null to discover local node
    private int discoverDestinationNode(XBeeAddress destination, XBeeNodeVisitor visitor) throws IOException {
        String desc = destination == null ? "local" : "remote";
        log.fine("Discover " + desc + " node information");
        XBeeFrameWithId[] responses = conn.sendFramesWithIdSeriallyAndWait(XBeeConnection.DEFAULT_TIMEOUT,
                XBeeAtFrame.newBuilder(destination).setAtCommand("SH"),
                XBeeAtFrame.newBuilder(destination).setAtCommand("SL"),
                XBeeAtFrame.newBuilder(destination).setAtCommand("MY"),
                XBeeAtFrame.newBuilder(destination).setAtCommand("NI"));
        int status = XBeeUtil.getStatus(responses);
        if (status != XBeeAtResponseFrame.STATUS_OK) {
            log.log(Level.SEVERE, "Failed to retrieve " + desc + " information: " + XBeeUtil.formatStatus(status));
            return status;
        }
        byte[] localNodeAddressBytes = new byte[10];
        System.arraycopy(responses[0].getData(), 0, localNodeAddressBytes, 0, 4);
        System.arraycopy(responses[1].getData(), 0, localNodeAddressBytes, 4, 4);
        System.arraycopy(responses[2].getData(), 0, localNodeAddressBytes, 8, 2);
        String localNodeId = HexUtil.formatAscii(responses[3].getData());
        XBeeNode node = new XBeeNode(XBeeAddress.valueOf(localNodeAddressBytes, 0), localNodeId, true);
        if (destination == null)
            synchronized (this) {
                localNode = node;
            }
        putNode(node);
        if (visitor != null)
            visitor.visitNode(node);
        return status;
    }

    // id=null to discover all remote nodes
    // timeout=0 to use min possible timeout
    private int discoverRemoteNode(String id, XBeeNodeVisitor visitor, long timeout) throws IOException {
        log.fine("Discover remote " + (id == null ? "nodes" : "node " + id));
        long discoveryTimeout = Math.max(MIN_DISCOVERY_TIMEOUT,
                Math.min(MAX_DISCOVERY_TIMEOUT, timeout / DISCOVERY_TIMEOUT_UNIT));
        XBeeFrameWithId[] responses = conn.waitResponses(XBeeConnection.DEFAULT_TIMEOUT, conn.sendFramesWithId(
                XBeeAtFrame.newBuilder().setAtCommand("NT").setData((byte)discoveryTimeout)));
        int status = XBeeUtil.getStatus(responses);
        if (status != XBeeAtResponseFrame.STATUS_OK) {
            log.log(Level.SEVERE, "Failed to set discovery timeout: " + XBeeUtil.formatStatus(status));
            return status;
        }
        NodeDiscoveryListener listener = new NodeDiscoveryListener(visitor);
        conn.addListener(XBeeNodeDescriptionContainer.class, listener);
        try {
            long tillTime = System.currentTimeMillis() + discoveryTimeout * DISCOVERY_TIMEOUT_UNIT;
            // wait for first response
            responses = conn.waitResponses(
                XBeeConnection.DEFAULT_TIMEOUT + discoveryTimeout * DISCOVERY_TIMEOUT_UNIT, conn.sendFramesWithId(
                        XBeeAtFrame.newBuilder().setAtCommand(XBeeNodeDiscoveryResponseFrame.NODE_DISCOVERY_COMMAND)
                                .setData(id == null ? new byte[0] : HexUtil.parseAscii(id))));
            status = XBeeUtil.getStatus(responses);
            if (status != XBeeAtResponseFrame.STATUS_OK)
                return status;
            // if all nodes are discovered, wait for the end of timeout
            if (id == null) {
                long waitTime = tillTime - System.currentTimeMillis();
                if (waitTime > 0)
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException e) {
                        throw ((InterruptedIOException) new InterruptedIOException().initCause(e));
                    }
            }
        } finally {
            conn.removeListener(XBeeNodeDescriptionContainer.class, listener);
        }
        return status;
    }

    private synchronized XBeeNode getLocalNode() {
        return localNode;
    }

    private synchronized XBeeNode getByNodeId(String id) {
        if (id == null)
            throw new NullPointerException();
        return nodeById.get(id);
    }

    private synchronized void putNode(XBeeNode node) {
        nodeById.put(node.getId(), node);
        nodeByAddress.put(node.getAddress(), node);
        notifyAll();
    }

    private XBeeNode checkStatus(int status, XBeeNode result) throws IOException {
        if (status != XBeeAtResponseFrame.STATUS_OK)
            throw new IOException(XBeeUtil.formatStatus(status));
        if (result == null)
            throw new IOException("Reason unknown");
        return result;
    }

    private class NodeDiscoveryListener implements XBeeFrameListener<XBeeNodeDescriptionContainer> {
        private final XBeeNodeVisitor visitor;

        public NodeDiscoveryListener(XBeeNodeVisitor visitor) {
            this.visitor = visitor;
        }

        public void frameReceived(XBeeNodeDescriptionContainer frame) {
            XBeeNodeDescription nd = frame.getDescription();
            XBeeNode node = new XBeeNode(nd.getAddress(), nd.getNodeId(), false);
            putNode(node);
            if (visitor != null)
                visitor.visitNode(node);
        }

        @Override
        public void connectionClosed() {}
    }
}
