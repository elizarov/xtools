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

/**
 * @author Roman Elizarov
 */
public class XBeeNodeDescription {
    static final int MIN_SIZE = 21;

    private byte[] frame;
    private int offset;

    XBeeNodeDescription(byte[] frame, int offset) {
        this.frame = frame;
        this.offset = offset;
    }

    public XBeeAddress getAddress() {
        byte[] bytes = new byte[10];
        System.arraycopy(frame, offset, bytes, 8, 2);
        System.arraycopy(frame, offset + 2, bytes, 0, 8);
        return XBeeAddress.valueOf(bytes, 0);
    }

    private int getNodeIdEndOffset() {
        int i = offset + 10;
        while (frame[i] != 0)
            i++;
        return i;
    }

    public String getNodeId() {
        return HexUtil.formatAscii(frame, offset + 10, getNodeIdEndOffset());
    }

    public short getParentNetworkAddress() {
        return HexUtil.getBigEndianShort(frame, getNodeIdEndOffset() + 1);
    }

    public byte getDeviceType() {
        return frame[getNodeIdEndOffset() + 3];
    }

    public short getProfileId() {
        return HexUtil.getBigEndianShort(frame, getNodeIdEndOffset() + 5);
    }

    public short getManufacturerId() {
        return HexUtil.getBigEndianShort(frame, getNodeIdEndOffset() + 7);
    }

    @Override
    public String toString() {
        return getAddress() + " " +
                getNodeId() + " " +
                HexUtil.formatNibbles(getParentNetworkAddress(), 4) + " " +
                HexUtil.formatByte(getDeviceType()) + " " +
                HexUtil.formatNibbles(getProfileId(), 4) + " " +
                HexUtil.formatNibbles(getManufacturerId(), 4);
    }
}
