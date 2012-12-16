package org.avrbuddy.xbee.discover;

import org.avrbuddy.hex.HexUtil;
import org.avrbuddy.log.Log;
import org.avrbuddy.xbee.api.*;

import java.io.IOException;
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

    public int discoverAllNodes(XBeeNodeVisitor visitor) throws IOException {
        return Math.max(
                discoverLocalNode(visitor),
                discoverRemoteNode(null, visitor, 0));
    }

    public XBeeNode getOrDiscoverLocalNode() throws IOException {
        if (getLocalNode() == null)
            discoverLocalNode(null);
        return getLocalNode();
    }

    public XBeeNode getOrDiscoverByNodeId(String id, int attempts) throws IOException {
        if (id == null)
            throw new NullPointerException();
        XBeeNode node = getByNodeId(id);
        for (int attempt = 0; node == null && attempt < attempts; attempt++) {
            discoverRemoteNode(id, null, 0);
            node = getByNodeId(id);
        }
        return node;
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

    private int discoverLocalNode(XBeeNodeVisitor visitor) throws IOException {
        log.fine("Discover local node information");
        XBeeFrameWithId[] responses = conn.waitResponses(XBeeConnection.DEFAULT_TIMEOUT, conn.sendFramesWithId(
                XBeeAtFrame.newBuilder().setAtCommand("SH"),
                XBeeAtFrame.newBuilder().setAtCommand("SL"),
                XBeeAtFrame.newBuilder().setAtCommand("MY"),
                XBeeAtFrame.newBuilder().setAtCommand("NI")));
        int status = conn.getStatus(responses);
        if (status != XBeeAtResponseFrame.STATUS_OK) {
            log.log(Level.SEVERE, "Failed to retrieve local node information: " + conn.fmtStatus(status));
            return status;
        }
        byte[] localNodeAddressBytes = new byte[10];
        System.arraycopy(responses[0].getData(), 0, localNodeAddressBytes, 0, 4);
        System.arraycopy(responses[1].getData(), 0, localNodeAddressBytes, 4, 4);
        System.arraycopy(responses[2].getData(), 0, localNodeAddressBytes, 8, 2);
        String localNodeId = HexUtil.formatAscii(responses[3].getData());
        localNode = new XBeeNode(XBeeAddress.valueOf(localNodeAddressBytes, 0), localNodeId, true);
        putNode(localNode);
        if (visitor != null)
            visitor.visitNode(localNode);
        return status;
    }

    // id=null to discover all remote nodes
    // timeout=0 to use min possible timeout
    private int discoverRemoteNode(String id, XBeeNodeVisitor visitor, long timeout) throws IOException {
        log.fine("Discover remote " + (id == null ? "nodes" : "node " + id));
        byte discoveryTimeout = (byte) Math.max(MIN_DISCOVERY_TIMEOUT,
                Math.min(MAX_DISCOVERY_TIMEOUT, timeout / DISCOVERY_TIMEOUT_UNIT));
        XBeeFrameWithId[] responses = conn.waitResponses(XBeeConnection.DEFAULT_TIMEOUT, conn.sendFramesWithId(
                XBeeAtFrame.newBuilder().setAtCommand("NT").setData(discoveryTimeout)));
        int status = conn.getStatus(responses);
        if (status != XBeeAtResponseFrame.STATUS_OK) {
            log.log(Level.SEVERE, "Failed to set discovery timeout: " + conn.fmtStatus(status));
            return status;
        }
        NodeDiscoveryListener listener = new NodeDiscoveryListener(visitor);
        conn.addListener(XBeeNodeDescriptionContainer.class, listener);
        responses = conn.waitResponses(XBeeConnection.DEFAULT_TIMEOUT + discoveryTimeout * DISCOVERY_TIMEOUT_UNIT,
                conn.sendFramesWithId(
                        XBeeAtFrame.newBuilder().setAtCommand(XBeeNodeDiscoveryResponseFrame.NODE_DISCOVERY_COMMAND)
                            .setData(id == null ? new byte[0] : HexUtil.parseAscii(id))));
        conn.removeListener(XBeeNodeDescriptionContainer.class, listener);
        status = conn.getStatus(responses);
        if (status != XBeeAtResponseFrame.STATUS_OK) {
            log.log(Level.SEVERE, "Failed to discover remote " + (id == null ? "nodes" : "node " + id) + ": " +
                    conn.fmtStatus(status));
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
