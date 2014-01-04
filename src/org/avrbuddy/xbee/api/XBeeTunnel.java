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

package org.avrbuddy.xbee.api;

import org.avrbuddy.conn.BufferedConnection;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author Roman Elizarov
 */
class XBeeTunnel extends BufferedConnection {
    private static final int BUFFER_SIZE = 8192;

    private final XBeeConnection conn;
    private final XBeeAddress destination;
    private final Listener listener = new Listener();

    public XBeeTunnel(XBeeConnection conn, XBeeAddress destination, int maxPayloadSize) {
        super(BUFFER_SIZE, maxPayloadSize);
        this.conn = conn;
        this.destination = destination;
        conn.addListener(XBeeRxFrame.class, listener);
    }

    @Override
    protected void flushOutput(byte[] buffer, int size) throws IOException {
        if (size > 0)
            conn.sendFrames(XBeeTxFrame.newBuilder(destination).setData(Arrays.copyOf(buffer, size)).build());
    }

    @Override
    protected void closeImpl() {
        conn.removeListener(XBeeRxFrame.class, listener);
    }

    @Override
    public void resetHost() throws IOException {
        conn.resetRemoteHost(destination);
    }

    @Override
    public String toString() {
        return "tunnel to " + destination;
    }

    private class Listener implements XBeeFrameListener<XBeeRxFrame> {
        public void frameReceived(XBeeRxFrame frame) {
            if (destination.equals(XBeeAddress.BROADCAST) || frame.getSource().equals(destination))
                in.write(frame.getData());
        }

        public void connectionClosed() {
            close();
        }
    }
}
