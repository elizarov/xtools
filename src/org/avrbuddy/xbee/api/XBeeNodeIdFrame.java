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
