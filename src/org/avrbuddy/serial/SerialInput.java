package org.avrbuddy.serial;

import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.TooManyListenersException;

/**
 * @author Roman Elizarov
 */
class SerialInput extends InputStream implements SerialPortEventListener {
    private final InputStream in;
    private long timeout;

    public SerialInput(SerialPort serialPort) throws IOException {
        this.in = serialPort.getInputStream();
        try {
            serialPort.addEventListener(this);
        } catch (TooManyListenersException e) {
            throw new IOException(e);
        }
        serialPort.notifyOnDataAvailable(true);
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

    public void serialEvent(SerialPortEvent event) {
        if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE)
            synchronized (this) {
                notifyAll();
            }
    }

    public synchronized void drain() throws IOException {
        while (in.available() > 0)
            in.read();
    }

    public synchronized void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}
