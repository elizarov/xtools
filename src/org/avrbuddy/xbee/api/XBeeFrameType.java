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

/**
 * @author Roman Elizarov
 */
public enum XBeeFrameType {
    MODEM_STATUS(0x8A),
    AT(0x08),
    AT_QUERY(0x09),
    AT_RESPONSE(0x88),
    REMOTE_AT(0x17),
    REMOTE_AT_RESPONSE(0x97),
    TX(0x10),
    EXPLICIT_TX(0x11),
    TX_STATUS(0x8B),
    RX(0x90),
    EXPLICIT_RX(0x91),
    IO_SAMPLE(0x92),
    SENSOR_READ(0x94),
    NODE_ID(0x95);

    private final byte frameType;

    XBeeFrameType(int cmdId) {
        this.frameType = (byte)cmdId;
    }

    public byte getFrameType() {
        return frameType;
    }

    public static XBeeFrameType forFrameType(byte frameType) {
        for (XBeeFrameType type : values())
            if (type.frameType == frameType)
                return type;
        return null;
    }
}
