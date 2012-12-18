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
public abstract class XBeeFrameWithId extends XBeeFrame {
    public XBeeFrameWithId(byte[] frame) {
        super(frame);
        if (frame.length < 5)
            throw new IllegalArgumentException(getFrameType() + " frame is too short");
    }

    public boolean isResponseFor(XBeeFrameWithId other) {
        return false;
    }

    public byte getFrameId() {
        return frame[4];
    }

    public abstract static class Builder {
        byte frameId;

        Builder() {
        }

        public abstract XBeeFrameWithId build();

        public byte getFrameId() {
            return frameId;
        }

        public Builder setFrameId(byte frameId) {
            this.frameId = frameId;
            return this;
        }
    }
}
