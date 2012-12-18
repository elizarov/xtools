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
public class XBeeRxFrame extends XBeeFrame {
    XBeeRxFrame(byte[] frame) {
        super(frame);
        if (frame.length < 16)
            throw new IllegalArgumentException(getFrameType() + " frame is too short");
    }

    public XBeeAddress getSource() {
        return XBeeAddress.valueOf(frame, 4);
    }

    public byte getOptions() {
        return frame[14];
    }

    public byte[] getData() {
        return Arrays.copyOfRange(frame, 15, frame.length - 1);
    }

    @Override
    public String toString() {
        return getFrameType() + " " +
                getSource() + " " +
                HexUtil.formatByte(getOptions()) + " " +
                HexUtil.formatAscii(frame, 15, frame.length - 1);
    }
}
