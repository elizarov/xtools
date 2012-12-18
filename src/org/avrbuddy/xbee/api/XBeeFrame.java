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

import org.avrbuddy.hex.HexUtil;

import java.io.ByteArrayOutputStream;

/**
 * @author Roman Elizarov
 */
public class XBeeFrame {
    final byte[] frame;

    public static XBeeFrame parse(byte[] frame) {
        if (frame.length <= 4)
            throw new IllegalArgumentException("Frame is too small");
        if (frame[0] != XBeeUtil.FRAME_START)
            throw new IllegalArgumentException("Missing frame start");
        if ((((frame[1] & 0xff) << 8) | (frame[2] & 0xff)) != computeFrameLength(frame))
            throw new IllegalArgumentException("Invalid frame length");
        if (frame[frame.length - 1] != computeFrameChecksum(frame))
            throw new IllegalArgumentException("Invalid frame checksum");
        XBeeFrameType type = XBeeFrameType.forFrameType(frame[3]);
        switch (type) {
        case AT:
            return new XBeeAtFrame(frame);
        case AT_RESPONSE:
            XBeeAtResponseFrame atResponseFrame = new XBeeAtResponseFrame(frame);
            if (atResponseFrame.getAtCommand().equals(XBeeNodeDiscoveryResponseFrame.NODE_DISCOVERY_COMMAND) &&
                    atResponseFrame.getStatus() == XBeeAtResponseFrame.STATUS_OK)
                atResponseFrame = new XBeeNodeDiscoveryResponseFrame(frame);
            return atResponseFrame;
        case REMOTE_AT:
            return new XBeeRemoteAtFrame(frame);
        case REMOTE_AT_RESPONSE:
            return new XBeeRemoteAtResponseFrame(frame);
        case TX:
            return new XBeeTxFrame(frame);
        case TX_STATUS:
            return new XBeeTxStatusFrame(frame);
        case RX:
            return new XBeeRxFrame(frame);
        case NODE_ID:
            return new XBeeNodeIdFrame(frame);
        default:
            return new XBeeFrame(frame);
        }
    }

    XBeeFrame(byte[] frame) {
        this.frame = frame;
    }

    public XBeeFrameType getFrameType() {
        return XBeeFrameType.forFrameType(frame[3]);
    }

    public byte[] getFrame() {
        return frame;
    }

    // specific to a frame type
    public byte getStatus() {
        return 0;
    }

    // specific to a frame type
    public byte[] getData() {
        return new byte[0];
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        XBeeFrameType frameType = getFrameType();
        if (frameType != null) {
            sb.append(frameType).append(' ');
            HexUtil.appendAscii(sb, frame, 4, frame.length - 1);
        } else {
            sb.append("UNKNOWN ");
            HexUtil.appendAscii(sb, frame, 3, frame.length - 1);
        }
        return sb.toString();
    }

    private static int computeFrameLength(byte[] frame) {
        return frame.length - 4;
    }

    private static byte computeFrameChecksum(byte[] frame) {
        byte sum = 0;
        for (int i = 3; i < frame.length - 1; i++) {
            sum += frame[i];
        }
        return (byte) (0xff - (sum & 0xff));
    }

    static Builder newBuilder(XBeeFrameType type) {
        return new Builder(type);
    }

    static class Builder {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        Builder(XBeeFrameType type) {
            out.write(XBeeUtil.FRAME_START);
            out.write(0);
            out.write(0);
            out.write(type.getFrameType());
        }

        public XBeeFrame build() {
            out.write(0);
            byte[] frame = out.toByteArray();
            int length = computeFrameLength(frame);
            frame[1] = (byte) (length >> 8);
            frame[2] = (byte) length;
            frame[frame.length - 1] = computeFrameChecksum(frame);
            return parse(frame);
        }

        public Builder append(int b) {
            out.write(b);
            return this;
        }

        public Builder append(byte[] b) {
            if (b != null)
                append(b, 0, b.length);
            return this;
        }

        public Builder append(byte[] b, int off, int len) {
            out.write(b, off, len);
            return this;
        }
    }
}
