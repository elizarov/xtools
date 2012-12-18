/*
 * Copyright (C) 2012 Roman Elizarov
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.avrbuddy.xbee.api;

import org.avrbuddy.serial.SerialConnection;
import org.avrbuddy.util.State;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * @author Roman Elizarov
 */
class XBeeTunnel extends SerialConnection {
    private static final int IN_BUFFER_SIZE = 1024;

    private static final int CLOSED = 1;
    private static final int DATA_AVAILABLE = 2;

    private final XBeeConnection conn;
    private final XBeeAddress destination;
    private final Input in;
    private final Output out;
    private final State state = new State();

    public XBeeTunnel(XBeeConnection conn, XBeeAddress destination, int maxPayloadSize) {
        this.conn = conn;
        this.destination = destination;
        in = new Input();
        out = new Output(maxPayloadSize);
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
        if (!state.set(CLOSED))
            return;
        conn.removeListener(XBeeRxFrame.class, in);
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
        private volatile long timeout;

        @Override
        public int available() {
            synchronized (buffer) {
                return state.is(CLOSED) ? 0 : (writeIndex - readIndex + IN_BUFFER_SIZE) % IN_BUFFER_SIZE;
            }
        }

        @Override
        public int read() throws IOException {
            while (true) {
                if (state.is(CLOSED))
                    throw new EOFException("Port is closed");
                state.await(DATA_AVAILABLE | CLOSED, timeout);
                synchronized (buffer) {
                    if (readIndex == writeIndex) {
                        state.clear(DATA_AVAILABLE);
                        continue;
                    }
                    byte b = buffer[readIndex];
                    readIndex = (readIndex + 1) % IN_BUFFER_SIZE;
                    return b & 0xff;
                }
            }
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return super.read(b, off, len);
        }

        void write(byte[] data) {
            int skipped = 0;
            synchronized (buffer) {
                for (int i = 0; i < data.length; i++) {
                    byte b = data[i];
                    int w2 = (writeIndex + 1) % IN_BUFFER_SIZE;
                    if (w2 == readIndex) {
                        skipped = data.length - i;
                        break;
                    }
                    buffer[writeIndex] = b;
                    writeIndex = w2;
                }
            }
            if (skipped > 0)
                log.warning("Skipped " + skipped + " bytes from " + destination + " due to input buffer overflow.");
            state.set(DATA_AVAILABLE);
        }

        public void frameReceived(XBeeRxFrame frame) {
            if (frame.getSource().equals(destination))
                write(frame.getData());
        }

        @Override
        public void connectionClosed() {
            close();
        }

        @Override
        public void close() {
            XBeeTunnel.this.close();
        }

        public void drain() {
            synchronized (buffer) {
                readIndex = 0;
                writeIndex = 0;
            }
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }
    }

    private class Output extends OutputStream {
        private final byte[] buffer;
        private int size;
        private volatile long timeout;

        public Output(int maxPayloadSize) {
            buffer = new byte[maxPayloadSize];
        }

        @Override
        public void write(int b) throws IOException {
            synchronized (buffer) {
                if (size >= buffer.length)
                    flush();
                buffer[size++] = (byte) b;
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            synchronized (buffer) {
                super.write(b, off, len);
            }
        }

        @Override
        public void flush() throws IOException {
            synchronized (buffer) {
                if (size > 0) {
                    conn.sendFrames(XBeeTxFrame.newBuilder(destination).setData(Arrays.copyOf(buffer, size)).build());
                    size = 0;
                }
            }
        }

        @Override
        public void close() {
            XBeeTunnel.this.close();
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }
    }
}
