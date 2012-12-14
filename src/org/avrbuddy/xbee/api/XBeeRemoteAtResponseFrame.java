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
