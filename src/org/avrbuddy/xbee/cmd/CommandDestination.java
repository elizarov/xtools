package org.avrbuddy.xbee.cmd;

import org.avrbuddy.xbee.api.XBeeAddress;
import org.avrbuddy.xbee.discover.XBeeNode;
import org.avrbuddy.xbee.discover.XBeeNodeDiscovery;

import java.io.IOException;

/**
 * @author Roman Elizarov
 */
public abstract class CommandDestination {
    public static final String NODE_LOCAL = ".";
    public static final String NODE_ID_PREFIX = "@";

    public static CommandDestination parse(String s) {
        s = s.trim();
        if (s.equals(XBeeAddress.COORDINATOR_STRING))
            return new Address(XBeeAddress.COORDINATOR);
        if (s.equals(XBeeAddress.BROADCAST_STRING))
            return new Address(XBeeAddress.BROADCAST);
        if (s.equals(NODE_LOCAL))
            return new Local();
        if (s.startsWith(NODE_ID_PREFIX))
            return new NodeId(s.substring(NODE_ID_PREFIX.length()));
        if (s.startsWith(XBeeAddress.S_PREFIX))
            try {
                return new Address(XBeeAddress.valueOf(s));
            } catch (IllegalArgumentException e) {
                throw new InvalidCommandException(e.getMessage());
            }
        return null;
    }

    public abstract XBeeNode resolveNode(CommandContext ctx) throws IOException;

    public XBeeAddress resolveAddress(CommandContext ctx) throws IOException {
        return resolveNode(ctx).getAddress();
    }

    public boolean isBroadcast() {
        return false;
    }

    public static class Address extends CommandDestination {
        private XBeeAddress address;

        public Address(XBeeAddress address) {
            this.address = address;
        }

        @Override
        public XBeeNode resolveNode(CommandContext ctx) throws IOException {
            return ctx.discovery.getOrDiscoverNodeByAddress(address);
        }

        @Override
        public XBeeAddress resolveAddress(CommandContext ctx) {
            return address;
        }

        @Override
        public boolean isBroadcast() {
            return address.equals(XBeeAddress.BROADCAST);
        }

        @Override
        public String toString() {
            return address.toString();
        }
    }

    public static class Local extends CommandDestination {
        @Override
        public XBeeNode resolveNode(CommandContext ctx) throws IOException {
            return ctx.discovery.getOrDiscoverLocalNode();
        }

        @Override
        public String toString() {
            return NODE_LOCAL;
        }
    }

    public static class NodeId extends CommandDestination {
        private final String nodeId;

        public NodeId(String nodeId) {
            this.nodeId = nodeId;
        }

        @Override
        public XBeeNode resolveNode(CommandContext ctx) throws IOException {
            XBeeNode node = ctx.discovery.getOrDiscoverByNodeId(nodeId, XBeeNodeDiscovery.DISCOVER_ATTEMPTS);
            if (node == null)
                throw new InvalidCommandException("Failed to discover remote node " + toString() +
                        " after " + XBeeNodeDiscovery.DISCOVER_ATTEMPTS + " attempts");
            return node;
        }

        @Override
        public String toString() {
            return NODE_ID_PREFIX + nodeId;
        }
    }
}
