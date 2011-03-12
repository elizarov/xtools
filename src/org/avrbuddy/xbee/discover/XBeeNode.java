package org.avrbuddy.xbee.discover;

import org.avrbuddy.xbee.api.XBeeAddress;

/**
 * @author Roman Elizarov
 */
public class XBeeNode {
    private XBeeAddress address;
    private String id;
    private boolean localNode;

    public XBeeNode(XBeeAddress address, String id, boolean localNode) {
        this.address = address;
        this.id = id;
        this.localNode = localNode;
    }

    public XBeeAddress getAddress() {
        return address;
    }

    public String getId() {
        return id;
    }

    public boolean isLocalNode() {
        return localNode;
    }

    @Override
    public String toString() {
        return address + " " + id + (localNode ? " local" : "");
    }
}
