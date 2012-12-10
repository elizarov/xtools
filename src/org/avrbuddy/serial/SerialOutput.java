package org.avrbuddy.serial;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Roman Elizarov
 */
class SerialOutput extends BufferedOutputStream {
    private volatile boolean enabled;
    private long timeout;

    public SerialOutput(OutputStream out) {
        super(out);
        this.out = out;
    }

    @Override
    public synchronized void write(int b) throws IOException {
        if (waitEnabled())
            super.write(b);
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        if (waitEnabled())
            super.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        if (waitEnabled())
            super.flush();
    }

    public synchronized void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled)
                notifyAll();
        }
    }

    private boolean waitEnabled() {
        if (enabled)
            return true;
        synchronized (this) {
            long time = System.currentTimeMillis();
            long till = time + timeout;
            while (!enabled && time < till) {
                try {
                    wait(till - time);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                time = System.currentTimeMillis();
            }
            return enabled;
        }
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}
