package org.avrbuddy.serial;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Roman Elizarov
 */
class SerialInput extends InputStream {
    private final InputStream in;
    private final AtomicBoolean notifyConnected = new AtomicBoolean();
    private volatile boolean closed;
    private volatile Runnable onConnected;
    private long timeout;

    public SerialInput(InputStream in) throws IOException {
        this.in = in;
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public synchronized int read() throws IOException {
        while (!closed && in.available() == 0) {
            checkNotifyConnected();
            synchronized (this) {
                try {
                    if (timeout == 0)
                        wait();
                    else {
                        wait(timeout);
                        break;
                    }
                } catch (InterruptedException e) {
                    throw new InterruptedIOException("interrupted");
                }
            }
        }
        if (closed)
            return -1;
        checkNotifyConnected();
        return in.read();
    }

    private void checkNotifyConnected() {
        if (!notifyConnected.get())
            return;
        if (notifyConnected.compareAndSet(true, false)) {
            Runnable onConnected = this.onConnected;
            if (onConnected != null)
                onConnected.run();
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (this) {
            if (closed)
                return;
            closed = true;
            notifyAll();
        }
        in.close();
    }

    public synchronized void connected() {
        notifyConnected.set(true);
        notifyAll();
    }

    public synchronized void dataAvailable() {
        notifyAll();
    }

    public void drain() throws IOException {
        while (in.available() > 0)
            in.read();
    }

    public synchronized void setTimeout(long timeout) {
        this.timeout = timeout;
        notifyAll();
    }

    public void setOnConnected(Runnable action) {
        onConnected = action;
    }
}
