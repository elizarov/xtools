package org.avrbuddy.serial;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;

/**
 * @author Roman Elizarov
 */
class SerialInput extends InputStream {
    private final InputStream in;
    private volatile Runnable portConnectionAction;
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
        while (in.available() == 0)
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
        return in.read();
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    public void connected() {
        Runnable action = portConnectionAction;
        if (action != null)
            action.run();
    }

    public synchronized void dataAvailable() {
        notifyAll();
    }

    public synchronized void drain() throws IOException {
        while (in.available() > 0)
            in.read();
    }

    public synchronized void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public void setPortConnectionAction(Runnable action) {
        portConnectionAction = action;
    }
}
