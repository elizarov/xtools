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

    public static final CommandDestination LOCAL = new Local();
    public static final CommandDestination COORDINATOR = new Address(XBeeAddress.COORDINATOR);
    public static final CommandDestination BROADCAST = new Address(XBeeAddress.BROADCAST);

    public static CommandDestination parse(String s) {
        s = s.trim();
        if (s.equals(XBeeAddress.COORDINATOR_STRING))
            return COORDINATOR;
        if (s.equals(XBeeAddress.BROADCAST_STRING))
            return BROADCAST;
        if (s.equals(NODE_LOCAL))
            return LOCAL;
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

    private static class Address extends CommandDestination {
        private XBeeAddress address;

        Address(XBeeAddress address) {
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

    private static class Local extends CommandDestination {
        Local() {}

        @Override
        public XBeeNode resolveNode(CommandContext ctx) throws IOException {
            return ctx.discovery.getOrDiscoverLocalNode();
        }

        @Override
        public String toString() {
            return NODE_LOCAL;
        }
    }

    private static class NodeId extends CommandDestination {
        private final String nodeId;

        NodeId(String nodeId) {
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
