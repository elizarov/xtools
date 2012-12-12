package org.avrbuddy.xbee.api;

import org.avrbuddy.serial.SerialConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Roman Elizarov
 */
class XBeeTunnel extends SerialConnection {
    private static final int IN_BUFFER_SIZE = 65536;
    private static final int OUT_PACKET_SIZE = 64;

    private final XBeeConnection conn;
    private final XBeeAddress destination;
    private final Input in = new Input();
    private final Output out = new Output();
    private final AtomicBoolean closed = new AtomicBoolean();

    public XBeeTunnel(XBeeConnection conn, XBeeAddress destination) {
        this.conn = conn;
        this.destination = destination;
        conn.addListener(XBeeRxFrame.class, in);
    }

    @Override
    public InputStream getInput() {
        return in;
    }

    @Override
    public OutputStream getOutput() {
        return out;
    }

    @Override
    public void close() {
        closeTunnel();
    }

    private void closeTunnel() {
        if (!closed.compareAndSet(false, true))
            return;
        conn.removeListener(XBeeRxFrame.class, in);
        in.close();
        out.close();
    }

    @Override
    public void resetHost() throws IOException {
        conn.resetRemoteHost(destination);
    }

    @Override
    public void drainInput() {
        in.drain();
    }

    @Override
    public void setReadTimeout(long timeout) throws IOException {
        in.setTimeout(timeout);
    }

    @Override
    public void setWriteTimeout(long timeout) throws IOException {
        out.setTimeout(timeout);
    }

    private class Input extends InputStream implements XBeeFrameListener<XBeeRxFrame> {
        private final byte[] buffer = new byte[IN_BUFFER_SIZE];
        private int readIndex = 0;
        private int writeIndex = 0;
        private int skipped = 0;
        private long timeout;
        private boolean closed;

        @Override
        public synchronized int available() throws IOException {
            return closed ? 0 : (writeIndex - readIndex + IN_BUFFER_SIZE) % IN_BUFFER_SIZE;
        }

        @Override
        public synchronized int read() throws IOException {
            while (!closed && readIndex == writeIndex)
                try {
                    if (timeout == 0)
                        wait();
                    else {
                        wait(timeout);
                        break;
                    }
                } catch (InterruptedException e) {
                    throw ((IOException) new InterruptedIOException().initCause(e));
                }
            if (closed || readIndex == writeIndex)
                return -1;
            byte b = buffer[readIndex];
            readIndex = (readIndex + 1) % IN_BUFFER_SIZE;
            return b & 0xff;
        }

        @Override
        public synchronized int read(byte[] b, int off, int len) throws IOException {
            return super.read(b, off, len);
        }

        private synchronized void write(byte[] data) {
            for (byte b : data) {
                write(b);
            }
            notifyAll();
            if (skipped > 0) {
                System.err.println("Skipped " + skipped + " bytes from " + destination + " due to input buffer overflow.");
                skipped = 0;
            }
        }

        private void write(byte b) {
            int w2 = (writeIndex + 1) % IN_BUFFER_SIZE;
            if (w2 == readIndex) {
                skipped++;
                return;
            }
            buffer[writeIndex] = b;
            writeIndex = w2;
        }

        public void frameReceived(XBeeRxFrame frame) {
            if (frame.getSource().equals(destination))
                write(frame.getData());
        }

        @Override
        public void connectionClosed() {
            closeTunnel();
        }

        @Override
        public void close() {
            synchronized (this) {
                if (closed)
                    return;
                closed = true;
                notifyAll();
            }
            closeTunnel();
        }

        public synchronized void drain() {
            readIndex = 0;
            writeIndex = 0;
        }

        public synchronized void setTimeout(long timeout) {
            this.timeout = timeout;
        }
    }

    private class Output extends OutputStream {
        private final byte[] buffer = new byte[OUT_PACKET_SIZE];
        private int size;
        private long timeout;

        @Override
        public synchronized void write(int b) throws IOException {
            if (size >= OUT_PACKET_SIZE)
                flush();
            buffer[size++] = (byte) b;
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) throws IOException {
            super.write(b, off, len);
        }

        @Override
        public synchronized void flush() throws IOException {
            if (size > 0) {
                conn.sendFramesWithId(XBeeTxFrame.newBuilder(destination).setData(Arrays.copyOf(buffer, size)));
                size = 0;
                // todo: wait for ACK or timeout
            }
        }

        @Override
        public void close() {
            closeTunnel();
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }
    }
}
