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
public class XBeeAtResponseFrame extends XBeeFrameWithId {
    public static final byte STATUS_OK = 0;
    public static final byte STATUS_ERROR = 1;
    public static final byte STATUS_INVALID_COMMAND = 2;
    public static final byte STATUS_INVALID_PARAMETER = 3;
    public static final byte STATUS_TX_FAILURE = 4;

    public XBeeAtResponseFrame(byte[] frame) {
        super(frame);
        if (frame.length < 9)
            throw new IllegalArgumentException(getFrameType() + " frame is too short");
    }

    @Override
    public boolean isResponseFor(XBeeFrameWithId other) {
        return other.getFrameId() == getFrameId() && other instanceof XBeeAtFrame &&
                ((XBeeAtFrame) other).getAtCommand().equals(getAtCommand());
    }

    public String getAtCommand() {
        return HexUtil.formatAscii(frame, 5, 7);
    }

    public byte getStatus() {
        return frame[7];
    }

    public byte[] getData() {
        return Arrays.copyOfRange(frame, 8, frame.length - 1);
    }

    public XBeeAddress getSource() {
        return null;
    }

    @Override
    public String toString() {
        String atCommand = getAtCommand();
        return getFrameType() + " " +
                HexUtil.formatByte(getFrameId()) + " " +
                atCommand + " " +
                HexUtil.formatByte(getStatus()) + " " +
                XBeeUtil.formatAtValue(atCommand, frame, 8, frame.length - 1);
    }
}
