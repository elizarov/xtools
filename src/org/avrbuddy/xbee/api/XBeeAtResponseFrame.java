package org.avrbuddy.xbee.api;

import org.avrbuddy.util.HexUtil;

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
