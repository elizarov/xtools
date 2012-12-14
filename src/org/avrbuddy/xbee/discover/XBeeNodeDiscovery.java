package org.avrbuddy.xbee.discover;

import org.avrbuddy.log.Log;
import org.avrbuddy.hex.HexUtil;
import org.avrbuddy.xbee.api.*;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author Roman Elizarov
 */
public class XBeeNodeDiscovery {
    private static final Logger log = Log.getLogger(XBeeNodeDiscovery.class);

    public static final int MIN_DISCOVERY_TIMEOUT = 0x20;
    public static final int MAX_DISCOVERY_TIMEOUT = 0xff;
    public static final int DISCOVER_ATTEMPTS = 3;

    private final XBeeConnection conn;
    private final AtResponseListener atResponseListener = new AtResponseListener();
    private final NodeDescriptionListener nodeDescriptionListener = new NodeDescriptionListener();

    private int discoveryTimeout = MIN_DISCOVERY_TIMEOUT;

    private int localNodeAddressParts;
    private byte[] localNodeAddressBytes = new byte[10];
    private String localNodeId;

    private XBeeNode localNode;
    private final Map<String, XBeeNode> nodeById = new HashMap<String, XBeeNode>();
    private final Map<XBeeAddress, XBeeNode> nodeByAddress = new HashMap<XBeeAddress, XBeeNode>();

    public XBeeNodeDiscovery(XBeeConnection conn) {
        this.conn = conn;
        conn.addListener(XBeeAtResponseFrame.class, atResponseListener);
        conn.addListener(XBeeNodeDescriptionContainer.class, nodeDescriptionListener);
    }

    public void close() {
        conn.removeListener(XBeeAtResponseFrame.class, atResponseListener);
        conn.removeListener(XBeeNodeDescriptionContainer.class, nodeDescriptionListener);
    }

    public int getDiscoveryTimeout() {
        return discoveryTimeout;
    }

    public void setDiscoveryTimeout(int discoveryTimeout) {
        if (discoveryTimeout < MIN_DISCOVERY_TIMEOUT || discoveryTimeout > MAX_DISCOVERY_TIMEOUT)
            throw new IllegalArgumentException();
        this.discoveryTimeout = discoveryTimeout;
    }

    public void discoverAllNodes() throws IOException {
        discoverLocalNode();
        discoverRemoteNode(null);
    }

    public XBeeNode getOrDiscoverLocalNode() throws IOException {
        return getOrDiscoverByNodeId(null, 1);
    }

    public XBeeNode getOrDiscoverByNodeId(String id, int attempts) throws IOException {
        XBeeNode node = getByNodeId(id);
        for (int attempt = 0; node == null && attempt < attempts; attempt++) {
            if (id != null) {
                discoverRemoteNode(id);
            } else {
                discoverLocalNode();
            }
            waitUntilDiscoveredOrTimeout(id);
            node = getByNodeId(id);
        }
        return node;
    }

    public synchronized XBeeNode getNodeByAddress(XBeeAddress address) {
        return nodeByAddress.get(address);
    }

    public void list() {
        ArrayList<XBeeNode> nodes;
        synchronized (this) {
            nodes = new ArrayList<XBeeNode>(nodeById.values());
        }
        Collections.sort(nodes);
        for (XBeeNode node : nodes)
            System.out.println(node);
    }

    // -------------- PRIVATE HELPER METHODS --------------

    private void discoverLocalNode() throws IOException {
        log.fine("Discover local node information");
        conn.sendFramesWithId(
                XBeeAtFrame.newBuilder().setAtCommand("SH"),
                XBeeAtFrame.newBuilder().setAtCommand("SL"),
                XBeeAtFrame.newBuilder().setAtCommand("MY"),
                XBeeAtFrame.newBuilder().setAtCommand("NI"));
    }

    private void discoverRemoteNode(String id) throws IOException {
        log.fine("Discover remote " + (id == null ? "nodes" : "node " + id));
        conn.sendFramesWithId(
                XBeeAtFrame.newBuilder().setAtCommand("NT").setData((byte) discoveryTimeout),
                XBeeAtFrame.newBuilder().setAtCommand(XBeeNodeDiscoveryResponseFrame.NODE_DISCOVERY_COMMAND)
                        .setData(id == null ? new byte[0] : HexUtil.parseAscii(id))
        );
    }

    private synchronized void waitUntilDiscoveredOrTimeout(String id) throws IOException {
        long timeout = discoveryTimeout * 100L;
        long time = System.currentTimeMillis();
        while (getByNodeId(id) == null && timeout > 0) {
            try {
                wait(timeout);
            } catch (InterruptedException e) {
                throw (IOException) new InterruptedIOException(e.getMessage()).initCause(e);
            }
            long nextTime = System.currentTimeMillis();
            timeout -= Math.max(1, nextTime - time);
            time = nextTime;
        }
    }

    private synchronized XBeeNode getByNodeId(String id) {
        return id == null ? localNode : nodeById.get(id);
    }

    private void putNode(XBeeNode node) {
        nodeById.put(node.getId(), node);
        nodeByAddress.put(node.getAddress(), node);
        notifyAll();
    }

    private void rebuildLocalNode() {
        if (localNodeAddressParts != 15)
            return;
        localNode = new XBeeNode(XBeeAddress.valueOf(localNodeAddressBytes, 0), localNodeId, true);
        putNode(localNode);
    }

    private synchronized void processAtResponseFrame(XBeeAtResponseFrame frame) {
        if (frame.getSource() != null || frame.getStatus() != XBeeAtResponseFrame.STATUS_OK)
            return;
        String cmd = frame.getAtCommand();
        byte[] data = frame.getData();
        if (cmd.equals("SH")) {
            System.arraycopy(data, 0, localNodeAddressBytes, 0, 4);
            localNodeAddressParts |= 1;
            rebuildLocalNode();
        } else if (cmd.equals("SL")) {
            System.arraycopy(data, 0, localNodeAddressBytes, 4, 4);
            localNodeAddressParts |= 2;
            rebuildLocalNode();
        } else if (cmd.equals("MY")) {
            System.arraycopy(data, 0, localNodeAddressBytes, 8, 2);
            localNodeAddressParts |= 4;
            rebuildLocalNode();
        } else if (cmd.equals("NI")) {
            localNodeId = HexUtil.formatAscii(data, 0, data.length);
            localNodeAddressParts |= 8;
            rebuildLocalNode();
        }
    }

    private synchronized void processNodeDescription(XBeeNodeDescription nd) {
        putNode(new XBeeNode(nd.getAddress(), nd.getNodeId(), false));
    }

    private class AtResponseListener implements XBeeFrameListener<XBeeAtResponseFrame> {
        public void frameReceived(XBeeAtResponseFrame frame) {
            processAtResponseFrame(frame);
        }

        @Override
        public void connectionClosed() {}
    }

    private class NodeDescriptionListener implements XBeeFrameListener<XBeeNodeDescriptionContainer> {
        public void frameReceived(XBeeNodeDescriptionContainer frame) {
            processNodeDescription(frame.getDescription());
        }

        @Override
        public void connectionClosed() {}
    }
}
