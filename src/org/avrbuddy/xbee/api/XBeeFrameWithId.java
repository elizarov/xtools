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
