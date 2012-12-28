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
public class AvrMemInfo {
    private final int memSize;
    private final int writeBlockSize;
    private final int readBlockSize;
    private final int addrDiv;

    public AvrMemInfo(int memSize, int writeBlockSize, int readBlockSize, int addrDiv) {
        this.memSize = memSize;
        this.writeBlockSize = writeBlockSize;
        this.readBlockSize = readBlockSize;
        this.addrDiv = addrDiv;
    }

    public int getMemSize() {
        return memSize;
    }

    public int getWriteBlockSize() {
        return writeBlockSize;
    }

    public int getReadBlockSize() {
        return readBlockSize;
    }

    public int getAddrDiv() {
        return addrDiv;
    }
}
