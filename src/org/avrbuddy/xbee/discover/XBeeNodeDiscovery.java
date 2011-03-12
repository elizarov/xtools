package org.avrbuddy.xbee.discover;

import org.avrbuddy.util.HexUtil;
import org.avrbuddy.xbee.api.*;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Roman Elizarov
 */
public class XBeeNodeDiscovery {
    public static final int MIN_DISCOVERY_TIMEOUT = 0x20;
    public static final int MAX_DISCOVERY_TIMEOUT = 0xff;

    private final XBeeConnection conn;
    private final XBeeFrameListener<XBeeAtResponseFrame> listener = new FrameListener();

    private int discoveryTimeout = MIN_DISCOVERY_TIMEOUT;

    private int localNodeAddressParts;
    private byte[] localNodeAddressBytes = new byte[10];
    private String localNodeId;

    private XBeeNode localNode;
    private final Map<String, XBeeNode> nodeById = new HashMap<String, XBeeNode>();
    private final Map<XBeeAddress, XBeeNode> nodeByAddress = new HashMap<XBeeAddress, XBeeNode>();
    private XBeeAddress orDiscoverLocalNode;

    public XBeeNodeDiscovery(XBeeConnection conn) {
        this.conn = conn;
        conn.addListener(XBeeAtResponseFrame.class, listener);
    }

    public void close() {
        conn.removeListener(XBeeAtResponseFrame.class, listener);
    }

    public int getDiscoveryTimeout() {
        return discoveryTimeout;
    }

    public void setDiscoveryTimeout(int discoveryTimeout) {
        if (discoveryTimeout < MIN_DISCOVERY_TIMEOUT || discoveryTimeout > MAX_DISCOVERY_TIMEOUT)
            throw new IllegalArgumentException();
        this.discoveryTimeout = discoveryTimeout;
    }

    public void discover() throws IOException {
        discoverLocalNode();
        discoverRemoteNode(null);
    }

    private void discoverLocalNode() throws IOException {
        conn.sendFramesWithId(
                XBeeAtFrame.newBuilder().setAtCommand("SH"),
                XBeeAtFrame.newBuilder().setAtCommand("SL"),
                XBeeAtFrame.newBuilder().setAtCommand("MY"),
                XBeeAtFrame.newBuilder().setAtCommand("NI"));
    }

    private void discoverRemoteNode(String id) throws IOException {
        conn.sendFramesWithId(
                XBeeAtFrame.newBuilder().setAtCommand("NT").setData(new byte[]{(byte) discoveryTimeout}),
                XBeeAtFrame.newBuilder().setAtCommand(XBeeNodeDiscoveryResponseFrame.NODE_DISCOVERY_COMMAND)
                        .setData(id == null ? new byte[0] : HexUtil.parseAscii(id))
        );
    }

    public XBeeNode getOrDiscoverLocalNode() throws IOException {
        return getOrDiscoverById(null);
    }

    public XBeeNode getOrDiscoverById(String id) throws IOException {
        XBeeNode node = getById(id);
        if (node != null)
            return node;
        if (id != null) {
            discoverRemoteNode(id);
        } else {
            discoverLocalNode();
        }
        long timeout = discoveryTimeout * 100L;
        synchronized (this) {
            node = getById(id);
            long time = System.currentTimeMillis();
            while (node == null && timeout > 0) {
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
        return node;
    }

    public XBeeNode getLocalNode() {
        return localNode;
    }

    public synchronized XBeeNode getById(String id) {
        return id == null ? localNode : nodeById.get(id);
    }

    public synchronized List<XBeeNode> getAllNodes() {
        return new ArrayList<XBeeNode>(nodeByAddress.values());
    }

    private void putNode(XBeeNode node) {
        nodeById.put(node.getId(), node);
        nodeByAddress.put(node.getAddress(), node);
        notify();
    }

    private void rebuildLocalNode() {
        if (localNodeAddressParts != 15)
            return;
        localNode = new XBeeNode(XBeeAddress.valueOf(localNodeAddressBytes, 0), localNodeId, true);
        putNode(localNode);
    }

    private synchronized void processFrame(XBeeAtResponseFrame frame) {
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
        } else if (cmd.equals(XBeeNodeDiscoveryResponseFrame.NODE_DISCOVERY_COMMAND)) {
            XBeeNodeDiscoveryResponseFrame nd = (XBeeNodeDiscoveryResponseFrame) frame;
            putNode(new XBeeNode(nd.getAddress(), nd.getNodeId(), false));
        }
    }

    private class FrameListener implements XBeeFrameListener<XBeeAtResponseFrame> {
        public void frameReceived(XBeeAtResponseFrame frame) {
            processFrame(frame);
        }
    }
}
