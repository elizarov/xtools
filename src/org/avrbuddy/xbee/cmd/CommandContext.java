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

import org.avrbuddy.conn.ConnectionOptions;
import org.avrbuddy.xbee.api.XBeeAddress;
import org.avrbuddy.xbee.api.XBeeConnection;
import org.avrbuddy.xbee.discover.XBeeNodeDiscovery;
import org.avrbuddy.xbee.link.XBeeLink;

import java.util.*;

/**
 * @author Roman Elizarov
 */
public class CommandContext {
    public final XBeeConnection conn;
    public final ConnectionOptions options;
    public final XBeeNodeDiscovery discovery;

    private final Map<XBeeAddress, List<XBeeLink>> links = new HashMap<XBeeAddress, List<XBeeLink>>();

    public CommandContext(XBeeConnection conn, ConnectionOptions options) {
        this.conn = conn;
        this.options = options;
        this.discovery = new XBeeNodeDiscovery(conn);
    }

    public void join() throws InterruptedException {
        for (XBeeLink link : getLinks())
            link.join();
    }

    public void close() {
        for (XBeeLink link : getLinks())
            link.close();
        conn.close();
    }

    public List<XBeeLink> getLinks() {
        List<XBeeLink> result = new ArrayList<XBeeLink>();
        synchronized (this) {
            for (List<XBeeLink> list : links.values())
                result.addAll(list);
        }
        return result;
    }

    public synchronized List<XBeeLink> getLinks(XBeeAddress remoteAddress) {
        if (remoteAddress == null)
            return getLinks();
        List<XBeeLink> result = links.get(remoteAddress);
        return result == null ? Collections.<XBeeLink>emptyList() : result;
    }

    public synchronized void addLink(XBeeLink link) {
        List<XBeeLink> list = links.get(link.getRemoteAddress());
        if (list == null)
            links.put(link.getRemoteAddress(), list = new ArrayList<XBeeLink>());
        list.add(link);
    }

    public synchronized void removeLink(XBeeLink link) {
        List<XBeeLink> list = links.get(link.getRemoteAddress());
        if (list == null)
            return;
        list.remove(link);
        if (list.isEmpty())
            links.remove(link.getRemoteAddress());
    }
}
