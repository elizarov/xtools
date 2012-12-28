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

/**
 * @author Roman Elizarov
 */
public enum AvrMemCmd {
    READ('R'),
    WRITE('W'),
    VERIFY('V');

    public static AvrMemCmd parse(String s) throws WrongFormatException {
        for (AvrMemCmd cmd : values())
            if (s.equalsIgnoreCase(cmd.codeStr))
                return cmd;
        throw new WrongFormatException("Unrecognized memory command '" + s + "'");
    }

    // -------------------------- instance --------------------------

    private final char code;
    private final String codeStr;

    private AvrMemCmd(char code) {
        this.code = code;
        codeStr = String.valueOf(code);
    }

    public char code() {
        return code;
    }

}
