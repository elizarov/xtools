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

import java.util.EnumMap;

/**
 * @author Roman Elizarov
 */
public enum AvrPart {
    ATMEGA168A (0x1e9406, new AvrMemInfo(16 * 1024, 128, 256, 2), new AvrMemInfo(512, 4, 256, 1)),
    ATMEGA168PA(0x1e940b, new AvrMemInfo(16 * 1024, 128, 256, 2), new AvrMemInfo(512, 4, 256, 1)),
    ATMEGA328  (0x1e9514, new AvrMemInfo(32 * 1024, 128, 256, 2), new AvrMemInfo(1024, 4, 256, 1)),
    ATMEGA328P (0x1e950f, new AvrMemInfo(32 * 1024, 128, 256, 2), new AvrMemInfo(1024, 4, 256, 1));

    public final int signature;
    public final EnumMap<AvrMemType, AvrMemInfo> memInfo = new EnumMap<AvrMemType, AvrMemInfo>(AvrMemType.class);

    private AvrPart(int signature, AvrMemInfo flash, AvrMemInfo eeprom) {
        this.signature = signature;
        memInfo.put(AvrMemType.FLASH, flash);
        memInfo.put(AvrMemType.EEPROM, eeprom);
    }

    public boolean hasSignature(byte[] signature) {
        return (this.signature >> 16) == (signature[0] & 0xff) &&
                ((this.signature >> 8) & 0xff) == (signature[1] & 0xff) &&
                (this.signature & 0xff) == (signature[2] & 0xff);
    }

    public AvrMemInfo getMemInfo(AvrMemType type) {
        return memInfo.get(type);
    }
}
