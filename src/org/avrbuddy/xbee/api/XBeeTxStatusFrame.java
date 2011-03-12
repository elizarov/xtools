package org.avrbuddy.xbee.api;

import org.avrbuddy.util.HexUtil;

/**
 * @author Roman Elizarov
 */
public class XBeeTxStatusFrame extends XBeeFrameWithId {
    public static final byte STATUS_OK = 0;
    public static final byte STATUS_ERROR = 1;
    public static final byte STATUS_INVALID_COMMAND = 2;
    public static final byte STATUS_INVALID_PARAMETER = 3;
    public static final byte STATUS_TX_FAILURE = 4;

    public XBeeTxStatusFrame(byte[] frame) {
        super(frame);
        if (frame.length < 10)
            throw new IllegalArgumentException(getFrameType() + " frame is too short");
    }

    @Override
    public boolean isResponseFor(XBeeFrameWithId other) {
        return other.getFrameId() == getFrameId() && other instanceof XBeeTxFrame;
    }

    public short getDestinationNetworkAddress() {
        return getShort(5);
    }

    public byte getTransmitRetryCount() {
        return frame[7];
    }

    public byte getDeliveryStatus() {
        return frame[8];
    }

    public byte getDiscoveryStatus() {
        return frame[9];
    }

    @Override
    public String toString() {
        return getFrameType() + " " +
                HexUtil.formatByte(getFrameId()) + " " +
                HexUtil.formatShort(getDestinationNetworkAddress()) + " " +
                HexUtil.formatByte(getTransmitRetryCount()) + " " +
                HexUtil.formatByte(getDeliveryStatus()) + " " +
                HexUtil.formatByte(getDiscoveryStatus());
    }
}
