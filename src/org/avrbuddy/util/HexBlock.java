package org.avrbuddy.util;

/**
 * @author Roman Elizarov
 */
public class HexBlock {
    private final int offset;
    private final byte[] data;

    public HexBlock(int offset, byte[] data) {
        this.offset = offset;
        this.data = data;
    }

    public int getOffset() {
        return offset;
    }

    public byte[] getData() {
        return data;
    }
}
