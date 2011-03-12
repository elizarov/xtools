package org.avrbuddy.xbee.api;

/**
 * @author Roman Elizarov
 */
public enum XBeeFrameType {
    MODEM_STATUS(0x8A),
    AT(0x08),
    AT_QUERY(0x09),
    AT_RESPONSE(0x88),
    REMOTE_AT(0x17),
    REMOTE_AT_RESPONSE(0x97),
    TX(0x10),
    EXPLICIT_TX(0x11),
    TX_STATUS(0x8B),
    RX(0x90),
    EXPLICIT_RX(0x91),
    IO_SAMPLE(0x92),
    SENSOR_READ(0x94),
    NODE_IDENTIFICATION(0x95);

    private final byte frameType;

    XBeeFrameType(int cmdId) {
        this.frameType = (byte)cmdId;
    }

    public byte getFrameType() {
        return frameType;
    }

    public static XBeeFrameType forFrameType(byte frameType) {
        for (XBeeFrameType type : values())
            if (type.frameType == frameType)
                return type;
        return null;
    }
}
