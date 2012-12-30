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

import org.avrbuddy.log.Log;

import java.io.*;
import java.util.logging.Logger;

/**
 * @author Roman Elizarov
 */
public class HexFile {
    private static final Logger log = Log.getLogger(HexFile.class);

    private static final String LINE_PREFIX = ":";
    private static final int TYPE_DATA = 0;
    private static final int TYPE_EOF = 1;
    private static final int WRITE_BLOCK_SIZE = 32;

    private int baseOffset = -1;
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

    public static HexFile read(File file) throws IOException {
        log.info("Reading file " + file);
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

    public static void write(File file, int baseOffset, byte[] bytes, int len) throws IOException {
        log.info("Writing file " + file);
        PrintWriter out = new PrintWriter(file);
        byte[] buf = new byte[5 + WRITE_BLOCK_SIZE];
        try {
            for (int ofs = 0; ofs < len; ofs += WRITE_BLOCK_SIZE) {
                int length = Math.min(WRITE_BLOCK_SIZE, len - ofs);
                int offset = baseOffset + ofs;
                if (offset > 0xffff)
                    throw new IOException("Offset is too large for Intel Hex file");
                buf[0] = (byte)length;
                buf[1] = (byte)(offset >> 8);
                buf[2] = (byte)offset;
                buf[3] = (byte)TYPE_DATA;
                System.arraycopy(bytes, ofs, buf, 4, length);
                writeLineWithCheckSum(out, buf, length + 4);
            }
            buf[0] = 0;
            buf[1] = 0;
            buf[2] = 0;
            buf[3] = (byte)TYPE_EOF;
            writeLineWithCheckSum(out, buf, 4);
        } finally {
            out.close();
        }
    }

    private static void writeLineWithCheckSum(PrintWriter out, byte[] buf, int len) {
        int sum = 0;
        for (int i = 0; i < len; i++)
            sum += buf[i] & 0xff;
        buf[len] = (byte)(-sum);
        out.print(LINE_PREFIX);
        out.println(HexUtil.formatBytes(buf, 0, len + 1));
    }

    // -------------------------- instance --------------------------

    private void parseLine(String line) throws IOException {
        if (line.length() == 0)
            return;
        if (!line.startsWith(LINE_PREFIX))
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
            throw new IOException("Invalid byte count.");
        if (type == TYPE_DATA) {
            byte[] b = HexUtil.parseBytes(line, 9, line.length() - 2);
            if (baseOffset < 0) {
                baseOffset = offset;
            } else if (baseOffset + bytes.size() != offset)
                throw new IOException("Blocks intersect or in invalid order");
            bytes.write(b);
        }
    }

    private HexFile() {
    }

    public boolean isEmpty() {
        return getLength() == 0;
    }

    public int getBaseOffset() {
        return baseOffset;
    }

    public int getLength() {
        return bytes.size();
    }

    public byte[] getBytes() {
        return bytes.toByteArray();
    }
}
