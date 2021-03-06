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

import org.avrbuddy.util.WrongFormatException;

import java.util.Locale;

/**
 * @author Roman Elizarov
 */
public enum AvrMemType {
    FLASH, EEPROM;

    public static AvrMemType parse(String s) throws WrongFormatException {
        for (AvrMemType type : values()) {
            String name = type.name();
            if (s.length() > 0 && s.length() <= name.length() && s.equalsIgnoreCase(name.substring(0, s.length())))
                return type;
        }
        throw new WrongFormatException("Unrecognized memory type '" + s + "'");
    }

    @Override
    public String toString() {
        String name = name().toLowerCase(Locale.US);
        return name.substring(0, 1) + "[" + name.substring(1) + "]";
    }

    public char code() {
        return name().charAt(0);
    }
}
