package org.avrbuddy.xbee.link;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
* @author Roman Elizarov
*/
class XBeeLinkThread extends Thread {
    private final InputStream in;
    private final OutputStream out;

    public XBeeLinkThread(String name, InputStream in, OutputStream out) {
        super(name);
        this.in = in;
        this.out = out;
    }

    @Override
    public void run() {
        byte[] buf = new byte[4096];
        try {
            while (true) {
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
            System.err.println(getName() + " I/O failed");
            e.printStackTrace();
            return;
        }
        System.err.println(getName() + " end of stream");
    }
}
