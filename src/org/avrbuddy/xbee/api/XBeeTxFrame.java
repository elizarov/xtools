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

import java.util.Arrays;

/**
 * @author Roman Elizarov
 */
public class XBeeTxFrame extends XBeeFrameWithId {
    XBeeTxFrame(byte[] frame) {
        super(frame);
        if (frame.length < 18)
            throw new IllegalArgumentException(getFrameType() + " frame is too short");
    }

    public XBeeAddress getDestination() {
        return XBeeAddress.valueOf(frame, 5);
    }

    public byte getBroadcastRadius() {
        return frame[15];
    }

    public byte getOptions() {
        return frame[16];
    }

    public byte[] getData() {
        return Arrays.copyOfRange(frame, 17, frame.length - 1);
    }

    @Override
    public String toString() {
        return getFrameType() + " " +
                HexUtil.formatByte(getFrameId()) + " " +
                getDestination() + " " +
                HexUtil.formatByte(getBroadcastRadius()) + " " +
                HexUtil.formatByte(getOptions()) + " " +
                HexUtil.formatAscii(frame, 17, frame.length - 1);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(XBeeAddress destination) {
        return new Builder().setDestination(destination);
    }

    public static class Builder extends XBeeFrameWithId.Builder {
        XBeeAddress destination;
        byte options;
        byte broadcastRadius;
        byte[] data;

        Builder() {}

        public XBeeTxFrame build() {
            return (XBeeTxFrame) XBeeFrame.newBuilder(XBeeFrameType.TX)
                    .append(frameId)
                    .append(destination.getAddressBytes())
                    .append(broadcastRadius)
                    .append(options)
                    .append(data)
                    .build();
        }

        public Builder setFrameId(byte frameId) {
            this.frameId = frameId;
            return this;
        }

        public XBeeAddress getDestination() {
            return destination;
        }

        public Builder setDestination(XBeeAddress destination) {
            if (destination == null)
                throw new NullPointerException();
            this.destination = destination;
            return this;
        }

        public byte getOptions() {
            return options;
        }

        public Builder setOptions(byte options) {
            this.options = options;
            return this;
        }

        public byte getBroadcastRadius() {
            return broadcastRadius;
        }

        public Builder setBroadcastRadius(byte broadcastRadius) {
            this.broadcastRadius = broadcastRadius;
            return this;
        }

        public byte[] getData() {
            return data;
        }

        public XBeeFrameWithId.Builder setData(byte[] data) {
            if (data == null)
                throw new NullPointerException();
            this.data = data;
            return this;
        }
    }
}
