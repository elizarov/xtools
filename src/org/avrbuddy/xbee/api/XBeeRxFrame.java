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
