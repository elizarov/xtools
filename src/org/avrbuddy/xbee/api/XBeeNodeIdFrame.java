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
public class XBeeNodeIdFrame extends XBeeFrame implements XBeeNodeDescriptionContainer {
    XBeeNodeIdFrame(byte[] frame) {
        super(frame);
        if (frame.length < 16 + XBeeNodeDescription.MIN_SIZE)
            throw new IllegalArgumentException(getFrameType() + " frame is too short");
    }

    public XBeeAddress getSource() {
        return XBeeAddress.valueOf(frame, 4);
    }

    public byte getOptions() {
        return frame[14];
    }

    public XBeeNodeDescription getDescription() {
        return new XBeeNodeDescription(frame, 15);
    }

    @Override
    public String toString() {
        return getFrameType() + " " +
                getSource() + " " +
                HexUtil.formatByte(getOptions()) + " " +
                getDescription();
    }
}
