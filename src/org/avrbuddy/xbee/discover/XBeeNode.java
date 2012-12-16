package org.avrbuddy.xbee.discover;

import org.avrbuddy.xbee.api.XBeeAddress;

/**
 * @author Roman Elizarov
 */
public class XBeeNode implements Comparable<XBeeNode> {
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
        return address + " @" + id + (localNode ? " ." : "");
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof XBeeNode && address.equals(((XBeeNode) o).address);
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }

    @Override
    public int compareTo(XBeeNode o) {
        return address.compareTo(o.address);
    }
}
