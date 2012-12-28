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

import org.avrbuddy.conn.Connection;
import org.avrbuddy.hex.HexUtil;
import org.avrbuddy.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Roman Elizarov
 */
public class AvrProgrammer {
    private static final Logger log = Log.getLogger(AvrProgrammer.class);

    private static final byte UNINITIALIZED = (byte) 0xff;

    private static final long READ_TIMEOUT = 500;
    private static final int MAX_ATTEMPTS = 10;

    private static final int SIGNATURE_LEN = 3;

    private static final byte STK_OK            = 0x10;
    private static final byte STK_INSYNC        = 0x14; // ' '
    private static final byte CRC_EOP           = 0x20; // 'SPACE'
    private static final byte STK_GET_SYNC      = 0x30; // '0'
    private static final byte STK_LOAD_ADDRESS  = 0x55; // 'U'
    private static final byte STK_PROG_PAGE     = 0x64; // 'd'
    private static final byte STK_READ_PAGE     = 0x74; // 't'
    private static final byte STK_READ_SIGN     = 0x75; // 'u'
    private static final byte STK_QUIT          = (byte) 'Q';

    public static AvrProgrammer open(Connection conn) throws IOException {
        conn.setReadTimeout(READ_TIMEOUT);
        for (int attempt = 0;; attempt++) {
            // connect limits number of reties with exception
            attempt = connect(conn, attempt);
            // read signature
            byte[] signature;
            try {
                signature = signature(conn);
            } catch (AvrSyncException e) {
                log.log(Level.WARNING, String.format("[%d] Cannot get AVR part signature", attempt + 1), e);
                continue;
            }
            for (AvrPart part : AvrPart.values()) {
                if (part.hasSignature(signature)) {
                    log.info("Connected to AVR bootloader for " + part);
                    return new AvrProgrammer(conn, part);
                }
            }
            throw new IOException("Unrecognized AVR part with signature " + HexUtil.formatBytes(signature));
        }
    }

    private static int connect(Connection conn, int attempt) throws IOException {
        for (; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                connectAttempt(conn);
                return attempt; // success
            } catch (AvrSyncException e) {
                log.log(Level.WARNING, String.format("[%d] Cannot connect to AVR bootloader", attempt + 1), e);
            }
        }
        throw new IOException(String.format("Cannot connect to AVR bootloader after %d attempts", MAX_ATTEMPTS));
    }

    private static void connectAttempt(Connection conn) throws IOException {
        conn.resetHost();
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            throw ((InterruptedIOException) new InterruptedIOException().initCause(e));
        }
        conn.drainInput();
        sync(conn);
    }

    private static void sync(Connection conn) throws IOException {
        OutputStream out = conn.getOutput();
        out.write(STK_GET_SYNC);
        out.write(CRC_EOP);
        out.flush();
        waitInSyncOk(conn);
    }

    private static void waitInSyncOk(Connection conn) throws IOException {
        waitInSyncOk(conn, null, 0, 0);
    }

    private static void waitInSyncOk(Connection conn, byte[] buf, int ofs, int len) throws IOException {
        InputStream in = conn.getInput();
        try {
            int b = in.read();
            if (b != STK_INSYNC)
                throw new AvrSyncException(String.format("Expected INSYNC (%02X) from AVR bootloader, got %02X", STK_INSYNC, b));
            while (len > 0) {
                int read = in.read(buf, ofs, len);
                if (read < 0)
                    throw new AvrSyncException("Connection closed while reading from AVR bootloader");
                ofs += read;
                len -= read;
            }
            b = in.read();
            if (b != STK_OK)
                throw new AvrSyncException(String.format("Expected OK (%02X) from AVR bootloader, got %02X", STK_OK, b));
        } catch (InterruptedIOException e) {
            throw new AvrSyncException("Read timed out while waiting for sync from AVR bootloader", e);
        }
    }

    private static byte[] signature(Connection conn) throws IOException {
        OutputStream out = conn.getOutput();
        out.write(STK_READ_SIGN);
        out.write(CRC_EOP);
        out.flush();
        byte[] signature = new byte[SIGNATURE_LEN];
        waitInSyncOk(conn, signature, 0, SIGNATURE_LEN);
        return signature;
    }

    // -------------------------- instance --------------------------

    private final Connection conn;
    private final AvrPart part;

    private int attempt;

    private AvrProgrammer(Connection conn, AvrPart part) {
        this.conn = conn;
        this.part = part;
    }

    public AvrPart getPart() {
        return part;
    }

    private void loadAddress(int addr) throws IOException {
        OutputStream out = conn.getOutput();
        out.write(STK_LOAD_ADDRESS);
        out.write(addr);
        out.write(addr >> 8);
        out.write(CRC_EOP);
    }

    private void checkAddress(AvrMemInfo memInfo, int baseOffset, byte[] bytes) throws IOException {
        if (baseOffset + bytes.length > memInfo.getMemSize())
            throw new IOException(String.format("End address %X is out of range %X", baseOffset + bytes.length, memInfo.getMemSize()));
    }

    private void reconnect() throws IOException {
        attempt = connect(conn, attempt);
    }

    public int read(AvrMemType memType, int baseOffset, byte[] bytes) throws IOException {
        AvrMemInfo memInfo = part.getMemInfo(memType);
        checkAddress(memInfo, baseOffset, bytes);
        int blockSize = memInfo.getReadBlockSize();
        log.info(String.format("Reading %d bytes from %s in blocks of %d bytes", bytes.length, memType.name(), blockSize));
        long time = System.currentTimeMillis();
        OutputStream out = conn.getOutput();
        for (int i = 0; i < bytes.length; i += blockSize) {
            int length = Math.min(blockSize, bytes.length - i);
            loadAddress((baseOffset + i) / memInfo.getAddrDiv());
            out.write(STK_READ_PAGE);
            out.write(length >> 8);
            out.write(length);
            out.write(memType.code());
            out.write(CRC_EOP);
            out.flush();
            try {
                waitInSyncOk(conn); // loadAddress
                waitInSyncOk(conn, bytes, i, length); // read
                attempt = 0; // reset retries counter after successful operation
            } catch (AvrSyncException e) {
                log.log(Level.WARNING, String.format("[%d] Cannot read block at %X", attempt + 1, baseOffset + i), e);
                reconnect();
                i -= blockSize; // retry block
            }
        }
        time = System.currentTimeMillis() - time;
        log.info(String.format(Locale.US, "Done reading in %.2f sec", time / 1000.0));
        // determine initialized length
        int len = bytes.length;
        while (len > 0 && bytes[len - 1] == UNINITIALIZED)
            len--;
        return len;
    }

    public void write(AvrMemType memType, int baseOffset, byte[] bytes) throws IOException {
        AvrMemInfo memInfo = part.getMemInfo(memType);
        checkAddress(memInfo, baseOffset, bytes);
        int blockSize = memInfo.getWriteBlockSize();
        log.info(String.format("Writing %d bytes from %s in blocks of %d bytes", bytes.length, memType.name(), blockSize));
        long time = System.currentTimeMillis();
        OutputStream out = conn.getOutput();
        for (int i = 0; i < bytes.length; i += blockSize) {
            int length = Math.min(blockSize, bytes.length - i);
            loadAddress((baseOffset + i) / memInfo.getAddrDiv());
            out.write(STK_PROG_PAGE);
            out.write(length >> 8);
            out.write(length);
            out.write(memType.code());
            out.write(bytes, i, length);
            out.write(CRC_EOP);
            out.flush();
            try {
                waitInSyncOk(conn); // loadAddress
                waitInSyncOk(conn); // write
                attempt = 0; // reset retries counter after successful operation
            } catch (AvrSyncException e) {
                log.log(Level.WARNING, String.format("[%d] Cannot write block at %X", attempt + 1, baseOffset + i), e);
                reconnect();
                i -= blockSize; // retry block
            }
        }
        time = System.currentTimeMillis() - time;
        log.info(String.format(Locale.US, "Done writing in %.2f sec", time / 1000.0));
    }

    public void quit() throws IOException {
        OutputStream out = conn.getOutput();
        out.write(STK_QUIT);
        out.write(CRC_EOP);
        out.flush();
    }

    public void close() {
        conn.close();
    }
}
