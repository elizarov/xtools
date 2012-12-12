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
    private static final int BUF_SIZE = 1024;
    private static final long AGGREGATION_DELAY = 2; // wait to produce larger packets

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
        byte[] buf = new byte[BUF_SIZE];
        try {
        main_loop:
            while (!Thread.interrupted()) {
                int first = in.read();
                if (first < 0)
                    break;
                buf[0] = (byte) first;
                int i = 1;
                while (i < BUF_SIZE) {
                    int available = in.available();
                    if (available == 0) {
                        try {
                            Thread.sleep(AGGREGATION_DELAY);
                        } catch (InterruptedException e) {
                            break main_loop;
                        }
                        // recheck after delay if still nothing available
                        available = in.available();
                        if (available == 0)
                            break;
                    }
                    int n = Math.min(BUF_SIZE - i, available);
                    n = in.read(buf, i, n);
                    if (n < 0)
                        break main_loop;
                    i += n;
                }
                out.write(buf, 0, i);
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
