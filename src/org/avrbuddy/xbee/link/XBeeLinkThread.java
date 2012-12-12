package org.avrbuddy.xbee.link;

import org.avrbuddy.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
* @author Roman Elizarov
*/
class XBeeLinkThread extends Thread {
    private static final Logger log = Log.getLogger(XBeeLinkThread.class);

    private final InputStream in;
    private final OutputStream out;
    private XBeeLinkThread other;

    public XBeeLinkThread(String name, InputStream in, OutputStream out) {
        super(name);
        this.in = in;
        this.out = out;
    }

    public void setOther(XBeeLinkThread other) {
        this.other = other;
    }

    private void close() {
        try {
            in.close();
        } catch (IOException e) {
            log.log(Level.WARNING, "Failed to close input", e);
        }
        try {
            out.close();
        } catch (IOException e) {
            log.log(Level.WARNING, "Failed to close output", e);
        }
    }

    @Override
    public void run() {
        byte[] buf = new byte[4096];
        try {
            while (!Thread.interrupted()) {
                int first = in.read();
                if (first < 0)
                    break;
                buf[0] = (byte) first;
                int n = Math.min(buf.length - 1, in.available());
                if (n > 0) {
                    n = in.read(buf, 1, n);
                    if (n < 0)
                        break;
                }
                out.write(buf, 0, n + 1);
                out.flush();
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed", e);
            return;
        }
        close();
        other.close();
    }
}
