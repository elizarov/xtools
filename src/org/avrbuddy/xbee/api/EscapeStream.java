package org.avrbuddy.xbee.api;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Roman Elizarov
 */
class EscapeStream extends OutputStream {
    private final OutputStream out;

    public EscapeStream(OutputStream out) {
        this.out = out;
    }

    @Override
    public void write(int b) throws IOException {
        switch (b) {
        case XBeeUtil.FRAME_START:
        case XBeeUtil.ESCAPE:
        case XBeeUtil.XON:
        case XBeeUtil.XOFF:
            out.write(XBeeUtil.ESCAPE);
            b = b ^ XBeeUtil.XOR;
        }
        out.write(b);
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }
}

