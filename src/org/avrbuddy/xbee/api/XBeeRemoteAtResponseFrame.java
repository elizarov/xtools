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
public class XBeeRemoteAtResponseFrame extends XBeeAtResponseFrame {
    XBeeRemoteAtResponseFrame(byte[] frame) {
        super(frame);
        if (frame.length < 19)
            throw new IllegalArgumentException(getFrameType() + " frame is too short");
    }

    @Override
    public boolean isResponseFor(XBeeFrameWithId other) {
        return super.isResponseFor(other) && other instanceof XBeeRemoteAtFrame;
    }

    public XBeeAddress getSource() {
        return XBeeAddress.valueOf(frame, 5);
    }

    public String getAtCommand() {
        return HexUtil.formatAscii(frame, 15, 17);
    }

    public byte getStatus() {
        return frame[17];
    }

    public byte[] getData() {
        return Arrays.copyOfRange(frame, 18, frame.length - 1);
    }

    @Override
    public String toString() {
        String atCommand = getAtCommand();
        return getFrameType() + " " +
                HexUtil.formatByte(getFrameId()) + " " +
                getSource() + " " +
                atCommand + " " +
                HexUtil.formatByte(getStatus()) + " " +
                XBeeUtil.formatAtValue(atCommand, frame, 18, frame.length - 1);
    }
}
