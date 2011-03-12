package org.avrbuddy.avr;

/**
 * @author Roman Elizarov
 */
public enum AvrDevice {
    ATMEGA168A (0x1e9406, 64, 4),
    ATMEGA168PA(0x1e940b, 64, 4),
    ATMEGA328  (0x1e9514, 64, 4),
    ATMEGA328P (0x1e950f, 64, 4);

    public final int signature;
    public final int flashPageSize;
    public final int eepromPageSize;

    AvrDevice(int signature, int flashPageSize, int eepromPageSize) {
        this.signature = signature;
        this.flashPageSize = flashPageSize;
        this.eepromPageSize = eepromPageSize;
    }

    public boolean hasSignature(byte[] signature) {
        return (this.signature >> 16) == (signature[0] & 0xff) &&
                ((this.signature >> 8) & 0xff) == (signature[1] & 0xff) &&
                (this.signature & 0xff) == (signature[2] & 0xff);
    }
}
