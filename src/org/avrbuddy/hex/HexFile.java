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

package org.avrbuddy.hex;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Roman Elizarov
 */
public class HexFile {
    private final List<HexBlock> blocks = new ArrayList<HexBlock>();

    public static HexFile read(File file) throws IOException {
        HexFile result = new HexFile();
        BufferedReader in = new BufferedReader(new FileReader(file));
        try {
            int lineNumber = 0;
            String line;
            while ((line = in.readLine()) != null) {
                lineNumber++;
                try {
                    result.parseLine(line);
                } catch (IOException e) {
                    throw new IOException("Invalid Intel Hex file: " + file + " at line " + lineNumber + ": " + e.getMessage(), e);
                }
            }
        } finally {
            in.close();
        }
        if (result.isEmpty())
            throw new IOException(file + ": No Intel HEX data records are found in file.");
        return result;
    }

    private void parseLine(String line) throws IOException {
        if (line.length() == 0)
            return;
        if (!line.startsWith(":"))
            throw new IOException("Line must start with ':'.");
        if (line.length() < 11)
            throw new IOException("Line is too short.");
        if (line.length() % 2 != 1)
            throw new IOException("Line should have an odd number of chars.");
        int sum = 0;
        for (int i = 1; i < line.length(); i += 2)
            try {
                sum += HexUtil.parseNibbles(line, i, i + 2);
            } catch (IllegalArgumentException e) {
                throw new IOException("Invalid character in line.");
            }
        if ((sum & 0xff) != 0)
            throw new IOException("Invalid line checksum.");
        int length = HexUtil.parseNibbles(line, 1, 3);
        int offset = HexUtil.parseNibbles(line, 3, 7);
        int type = HexUtil.parseNibbles(line, 7, 9);
        if (length * 2 != line.length() - 11)
            throw new InvalidObjectException("Invalid byte count.");
        if (type == 0)
            add(new HexBlock(offset, HexUtil.parseBytes(line, 9, line.length() - 2)));
    }

    private void add(HexBlock block) throws IOException {
        if (!blocks.isEmpty()) {
            HexBlock last = blocks.get(blocks.size() - 1);
            if (block.getOffset() < last.getOffset() + last.getData().length)
                throw new IOException("Blocks intersect or in invalid order.");
        }
        blocks.add(block);
    }

    private HexFile() {
    }

    public boolean isEmpty() {
        return blocks.isEmpty();
    }

    public List<HexBlock> getBlocks() {
        return blocks;
    }
}
