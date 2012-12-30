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

import org.avrbuddy.conn.Connection;
import org.avrbuddy.log.Log;
import org.avrbuddy.util.State;
import org.avrbuddy.xbee.api.XBeeAddress;
import org.avrbuddy.xbee.cmd.CommandConnection;
import org.avrbuddy.xbee.cmd.CommandContext;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Roman Elizarov
 */
public class XBeeLink {
    private static final Logger log = Log.getLogger(XBeeLink.class);

    private static final long WRITE_TIMEOUT = 100; // 100ms

    private static final int STARTED = 1;
    private static final int CLOSED = 2;

    private final CommandContext ctx;
    private final XBeeAddress remoteAddress;
    private final CommandConnection linkConnection;

    private final State state = new State();

    private Connection link;
    private Connection tunnel;
    private XBeeLinkThread remote2link;
    private XBeeLinkThread link2remote;

    public XBeeLink(CommandContext ctx, XBeeAddress remoteAddress, CommandConnection linkConnection) {
        this.ctx = ctx;
        this.remoteAddress = remoteAddress;
        this.linkConnection = linkConnection;
    }

    public CommandConnection getLinkConnection() {
        return linkConnection;
    }

    public XBeeAddress getRemoteAddress() {
        return remoteAddress;
    }

    public void start() throws IOException {
        if (!state.set(STARTED))
            return;
        if (state.is(CLOSED))
            return;

        ctx.conn.changeRemoteDestination(remoteAddress, ctx.discovery.getOrDiscoverLocalNode().getAddress());

        link = linkConnection.openConnection(ctx);
        tunnel = ctx.conn.openTunnel(remoteAddress);

        link.setWriteTimeout(WRITE_TIMEOUT);
        tunnel.setWriteTimeout(WRITE_TIMEOUT);

        link.setOnConnected(new Reset(tunnel));

        remote2link = new XBeeLinkThread("remote->link", tunnel.getInput(), link.getOutput());
        link2remote = new XBeeLinkThread("link->remote", link.getInput(), tunnel.getOutput());

        remote2link.setOther(link2remote);
        link2remote.setOther(remote2link);

        remote2link.start();
        link2remote.start();

        ctx.addLink(this);
    }

    public void join() throws InterruptedException {
        if (remote2link != null)
            remote2link.join();
        if (link2remote != null)
            link2remote.join();
    }

    public void close() {
        if (!state.set(CLOSED))
            return;
        if (tunnel != null)
            tunnel.close();
        if (link != null)
            link.close();
        ctx.removeLink(this);
    }

    private static class Reset implements Runnable {
        private final Connection tunnel;

        public Reset(Connection tunnel) {
            this.tunnel = tunnel;
        }

        @Override
        public void run() {
            log.info("Connection to link port established, resetting remote host");
            try {
                tunnel.resetHost();
            } catch (IOException e) {
                log.log(Level.SEVERE, "Failed to reset remote host", e);
            }
        }
    }
}
