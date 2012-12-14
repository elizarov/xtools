package org.avrbuddy.xbee.api;

import org.avrbuddy.hex.HexUtil;

/**
 * @author Roman Elizarov
 */
public class XBeeNodeDiscoveryResponseFrame extends XBeeAtResponseFrame implements XBeeNodeDescriptionContainer {
    public static final String NODE_DISCOVERY_COMMAND = "ND";

    XBeeNodeDiscoveryResponseFrame(byte[] frame) {
        super(frame);
        if (frame.length < 9 + XBeeNodeDescription.MIN_SIZE)
            throw new IllegalArgumentException(getFrameType() + " frame is too short");
    }

    public XBeeNodeDescription getDescription() {
        return new XBeeNodeDescription(frame, 8);
    }

    @Override
    public String toString() {
        return getFrameType() + " " +
                HexUtil.formatByte(getFrameId()) + " " +
                getAtCommand() + " " +
                HexUtil.formatByte(getStatus()) + " " +
                getDescription();
    }
}

