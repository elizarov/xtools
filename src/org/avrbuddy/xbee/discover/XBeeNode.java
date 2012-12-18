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
