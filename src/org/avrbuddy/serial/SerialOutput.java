package org.avrbuddy.serial;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Roman Elizarov
 */
class SerialOutput extends OutputStream {
    private final OutputStream out;
    private volatile boolean enabled;
    private long timeout;

    public SerialOutput(OutputStream out) {
        this.out = out;
    }

    @Override
    public void write(int b) throws IOException {
        if (waitEnabled())
            out.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (waitEnabled())
            out.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        if (waitEnabled())
            out.flush();
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
