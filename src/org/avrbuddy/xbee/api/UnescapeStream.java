package org.avrbuddy.xbee.api;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Roman Elizarov
 */
class UnescapeStream extends InputStream {
    private final InputStream in;

    UnescapeStream(InputStream in) {
        this.in = in;
    }

    @Override
    public int read() throws IOException {
        int b = in.read();
        if (b == XBeeUtil.ESCAPE) {
            b = in.read();
            if (b >= 0)
                b ^= XBeeUtil.XOR;
        }
        return b;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
