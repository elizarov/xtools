package org.avrbuddy.xbee.api;

import org.avrbuddy.hex.HexUtil;

import java.util.Arrays;

/**
 * @author Roman Elizarov
 */
public class XBeeRemoteAtFrame extends XBeeAtFrame {
    public static final byte APPLY_OPTION = 0x02;

    XBeeRemoteAtFrame(byte[] frame) {
        super(frame);
        if (frame.length < 19)
            throw new IllegalArgumentException(getFrameType() + " frame is too short");
    }

    public XBeeAddress getDestination() {
        return XBeeAddress.valueOf(frame, 5);
    }

    public byte getOptions() {
        return frame[15];
    }

    public String getAtCommand() {
        return HexUtil.formatAscii(frame, 16, 18);
    }

    public byte[] getData() {
        return Arrays.copyOfRange(frame, 18, frame.length - 1);
    }

    @Override
    public String toString() {
        String atCommand = getAtCommand();
        return getFrameType() + " " +
                HexUtil.formatByte(getFrameId()) + " " +
                getDestination() + " " +
                atCommand + " " +
                HexUtil.formatByte(getOptions()) + " " +
                XBeeUtil.formatAtValue(atCommand, frame, 18, frame.length - 1);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(XBeeAddress destination) {
        return newBuilder()
                .setDestination(destination)
                .setOptions(APPLY_OPTION);
    }

    public static class Builder extends XBeeAtFrame.Builder {
        XBeeAddress destination;
        byte options;

        public XBeeRemoteAtFrame build() {
            byte[] cmdBytes = HexUtil.parseAscii(atCommand);
            if (cmdBytes.length != 2)
                throw new IllegalArgumentException("Invalid AT Command: " + atCommand);
            return (XBeeRemoteAtFrame) XBeeFrame.newBuilder(XBeeFrameType.REMOTE_AT)
                    .append(frameId)
                    .append(destination.getAddressBytes())
                    .append(options)
                    .append(cmdBytes)
                    .append(data)
                    .build();
        }

        public Builder setFrameId(byte frameId) {
            this.frameId = frameId;
            return this;
        }

        public XBeeAddress getDestination() {
            return destination;
        }

        public Builder setDestination(XBeeAddress destination) {
            if (destination == null)
                throw new NullPointerException();
            this.destination = destination;
            return this;
        }

        public byte getOptions() {
            return options;
        }

        public Builder setOptions(byte options) {
            this.options = options;
            return this;
        }

        public Builder setAtCommand(String atCommand) {
            if (atCommand == null)
                throw new NullPointerException();
            this.atCommand = atCommand;
            return this;
        }

        public Builder setData(byte[] data) {
            if (data == null)
                throw new NullPointerException();
            this.data = data;
            return this;
        }
    }
}
