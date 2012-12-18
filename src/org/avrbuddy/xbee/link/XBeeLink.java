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

package org.avrbuddy.xbee.link;

import org.avrbuddy.log.Log;
import org.avrbuddy.serial.SerialConnection;
import org.avrbuddy.xbee.api.XBeeConnection;
import org.avrbuddy.xbee.discover.XBeeNode;
import org.avrbuddy.xbee.discover.XBeeNodeDiscovery;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Roman Elizarov
 */
public class XBeeLink {
    private static final Logger log = Log.getLogger(XBeeLink.class);

    private static final long WRITE_TIMEOUT = 100; // 100ms

    public static void main(String[] args) throws InterruptedException {
        Log.init(XBeeLink.class);
        if (args.length != 4) {
            log.log(Level.SEVERE, "Usage: " + XBeeLink.class.getName() + " <XBee-port> <baud> <link-node-id> <link-port>");
            return;
        }
        String port = args[0];
        int baud = Integer.parseInt(args[1]);
        String linkNodeId = args[2];
        String linkPort = args[3];
        try {
            XBeeConnection conn = XBeeConnection.open(SerialConnection.open(port, baud));
            try {
                new XBeeLink(conn, baud, linkNodeId, linkPort).go();
            } finally {
                conn.close();
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed", e);
        }
    }

    private final XBeeConnection conn;
    private final int baud;
    private final String linkNodeId;
    private final String linkPort;

    public XBeeLink(XBeeConnection conn, int baud, String linkNodeId, String linkPort) {
        this.conn = conn;
        this.baud = baud;
        this.linkNodeId = linkNodeId;
        this.linkPort = linkPort;
    }

    private void go() throws IOException, InterruptedException {
        XBeeNodeDiscovery discovery = new XBeeNodeDiscovery(conn);
        XBeeNode linkNode = discovery.getOrDiscoverByNodeId(linkNodeId, XBeeNodeDiscovery.DISCOVER_ATTEMPTS);
        if (linkNode == null) {
            log.log(Level.SEVERE, "Failed to discover link node @" + linkNodeId);
            return;
        }
        XBeeNode localNode = discovery.getOrDiscoverLocalNode();
        if (localNode == null) {
            log.log(Level.SEVERE, "Failed to resolve local node");
            return;
        }
        conn.changeRemoteDestination(linkNode.getAddress(), localNode.getAddress());

        SerialConnection tunnel = conn.openTunnel(linkNode.getAddress());
        SerialConnection link = SerialConnection.open(linkPort, baud);

        tunnel.setWriteTimeout(WRITE_TIMEOUT);
        link.setWriteTimeout(WRITE_TIMEOUT);

        link.setOnConnected(new Reset(tunnel));

        XBeeLinkThread remote2link = new XBeeLinkThread("remote->link", tunnel.getInput(), link.getOutput());
        XBeeLinkThread link2remote = new XBeeLinkThread("link->remote", link.getInput(), tunnel.getOutput());

        remote2link.setOther(link2remote);
        link2remote.setOther(remote2link);

        remote2link.start();
        link2remote.start();

        remote2link.join();
        link2remote.join();

        tunnel.close();
        link.close();
    }

    private static class Reset implements Runnable {
        private final SerialConnection tunnel;

        public Reset(SerialConnection tunnel) {
            this.tunnel = tunnel;
        }

        @Override
        public void run() {
            log.log(Level.SEVERE, "Connection to link port established, resetting remote host");
            try {
                tunnel.resetHost();
            } catch (IOException e) {
                log.log(Level.SEVERE, "Failed to reset remote host", e);
            }
        }
    }
}
