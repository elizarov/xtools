package org.avrbuddy.xbee.api;

import org.avrbuddy.util.HexUtil;

/**
 * @author Roman Elizarov
 */
public class XBeeNodeDiscoveryResponseFrame extends XBeeAtResponseFrame {
    public static final String NODE_DISCOVERY_COMMAND = "ND";

    XBeeNodeDiscoveryResponseFrame(byte[] frame) {
        super(frame);
        if (frame.length < 27)
            throw new IllegalArgumentException(getFrameType() + " frame is too short");
    }

    public XBeeAddress getAddress() {
        byte[] bytes = new byte[10];
        System.arraycopy(frame, 8, bytes, 8, 2);
        System.arraycopy(frame, 10, bytes, 0, 8);
        return XBeeAddress.valueOf(bytes, 0);
    }

    private int getNodeIdEndOffset() {
        int i = 18;
        while (frame[i] != 0)
            i++;
        return i;
    }

    public String getNodeId() {
        return HexUtil.formatAscii(frame, 18, getNodeIdEndOffset());
    }

    public short getParentNetworkAddress() {
        return getShort(getNodeIdEndOffset() + 1);
    }

    public byte getDeviceType() {
        return frame[getNodeIdEndOffset() + 3];
    }

    public short getProfileId() {
        return getShort(getNodeIdEndOffset() + 5);
    }

    public short getManufacturerId() {
        return getShort(getNodeIdEndOffset() + 7);
    }

    @Override
    public String toString() {
        return getFrameType() + " " +
                HexUtil.formatByte(getFrameId()) + " " +
                getAtCommand() + " " +
                HexUtil.formatByte(getStatus()) + " " +
                getAddress() + " " +
                getNodeId() + " " +
                HexUtil.formatShort(getParentNetworkAddress()) + " " +
                HexUtil.formatByte(getDeviceType()) + " " +
                HexUtil.formatShort(getProfileId()) + " " +
                HexUtil.formatShort(getManufacturerId());
    }
}

