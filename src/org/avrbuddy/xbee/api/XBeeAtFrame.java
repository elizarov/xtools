package org.avrbuddy.xbee.api;

import org.avrbuddy.util.HexUtil;

import java.util.Arrays;

/**
 * @author Roman Elizarov
 */
public class XBeeAtFrame extends XBeeFrameWithId {
    XBeeAtFrame(byte[] frame) {
        super(frame);
        if (frame.length < 8)
            throw new IllegalArgumentException(getFrameType() + " frame is too short");
    }

    public String getAtCommand() {
        return HexUtil.formatAscii(frame, 5, 7);
    }

    public byte[] getData() {
        return Arrays.copyOfRange(frame, 7, frame.length - 1);
    }

    public XBeeAddress getDestination() {
        return null;
    }

    @Override
    public String toString() {
        String atCommand = getAtCommand();
        return getFrameType() + " " +
                HexUtil.formatByte(getFrameId()) + " " +
                atCommand + " " +
                XBeeUtil.formatAtValue(atCommand, frame, 7, frame.length - 1);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(XBeeAddress destination) {
        return destination == null ? new Builder() : XBeeRemoteAtFrame.newBuilder(destination);
    }

    public static class Builder extends XBeeFrameWithId.Builder {
        String atCommand;
        byte[] data;

        public XBeeAtFrame build() {
            byte[] cmdBytes = HexUtil.parseAscii(atCommand);
            if (cmdBytes.length != 2)
                throw new IllegalArgumentException("Invalid AT Command: " + atCommand);
            return (XBeeAtFrame) XBeeFrame.newBuilder(XBeeFrameType.AT)
                    .append(frameId)
                    .append(cmdBytes)
                    .append(data)
                    .build();
        }

        public Builder setFrameId(byte frameId) {
            this.frameId = frameId;
            return this;
        }

        public String getAtCommand() {
            return atCommand;
        }

        public Builder setAtCommand(String atCommand) {
            this.atCommand = atCommand;
            return this;
        }

        public byte[] getData() {
            return data;
        }

        public XBeeFrameWithId.Builder setData(byte[] data) {
            if (data == null)
                throw new NullPointerException();
            this.data = data;
            return this;
        }
    }
}
