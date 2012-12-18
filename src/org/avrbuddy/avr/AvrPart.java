/*
 * Copyright (C) 2012 Roman Elizarov
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.avrbuddy.avr;

/**
 * @author Roman Elizarov
 */
public enum AvrPart {
    ATMEGA168A (0x1e9406, 64, 4),
    ATMEGA168PA(0x1e940b, 64, 4),
    ATMEGA328  (0x1e9514, 64, 4),
    ATMEGA328P (0x1e950f, 64, 4);

    public final int signature;
    public final int flashPageSize;
    public final int eepromPageSize;

    AvrPart(int signature, int flashPageSize, int eepromPageSize) {
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
