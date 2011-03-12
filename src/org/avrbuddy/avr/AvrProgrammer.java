package org.avrbuddy.avr;

import org.avrbuddy.serial.SerialConnection;
import org.avrbuddy.util.HexUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Roman Elizarov
 */
public class AvrProgrammer {
    private final SerialConnection serial;
    private final AvrDevice device;

    private static final byte STK_OK            = 0x10;
    private static final byte STK_INSYNC        = 0x14; // ' '
    private static final byte CRC_EOP           = 0x20; // 'SPACE'
    private static final byte STK_GET_SYNC      = 0x30; // '0'
    private static final byte STK_PROG_PAGE     = 0x64; // 'd'
    private static final byte STK_READ_PAGE     = 0x74; // 't'
    private static final byte STK_READ_SIGN     = 0x75; // 'u'

    public static AvrProgrammer open(SerialConnection serial) throws IOException {
        serial.drainInput();
        sync(serial);
        byte[] signature = signature(serial);
        for (AvrDevice device : AvrDevice.values()) {
            if (device.hasSignature(signature))
                return new AvrProgrammer(serial, device);
        }
        throw new IOException("Unknown device signature " + HexUtil.formatBytes(signature, 0, 3));
    }

    private static void sync(SerialConnection serial) throws IOException {
        OutputStream out = serial.getOutput();
        InputStream in = serial.getInput();
        out.write(STK_GET_SYNC);
        out.write(CRC_EOP);
        out.flush();
        if (in.read() != STK_INSYNC || in.read() != STK_OK)
            throw new AvrSyncException("Cannot get sync with AVR device");
    }

    private static byte[] signature(SerialConnection serial) throws IOException {
        OutputStream out = serial.getOutput();
        InputStream in = serial.getInput();
        out.write(STK_READ_SIGN);
        out.write(CRC_EOP);
        out.flush();
        if (in.read() != STK_INSYNC)
            throw new AvrSyncException("Cannot get sync with AVR device");
        byte[] signature = new byte[3];
        if (in.read(signature) != 3 || in.read() != STK_OK)
            throw new IOException("Cannot read AVR device signature");
        return signature;
    }

    public AvrProgrammer(SerialConnection serial, AvrDevice device) {
        this.serial = serial;
        this.device = device;
    }

    public AvrDevice getDevice() {
        return device;
    }

    public void close() {
        serial.close();
    }
}
