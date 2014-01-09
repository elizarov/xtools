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

package org.avrbuddy.conn;

import org.avrbuddy.util.State;

import java.io.*;

/**
 * @author Roman Elizarov
 */
public abstract class BufferedConnection extends Connection {
    protected static final int CLOSED = 1;
    protected static final int DATA_AVAILABLE = 2;
    protected static final int END_OF_INPUT = 4;

    protected final Input in;
    protected final Output out;
    protected final State state = new State();

    public BufferedConnection(int inBufferSize, int outBufferSize) {
        in = new Input(inBufferSize);
        out = new Output(outBufferSize);
    }

    protected abstract void flushOutput(byte[] buffer, int size) throws IOException;

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
        closeImpl();
    }

    protected void closeImpl() {}

    @Override
    public void drainInput() {
        in.drain();
    }

    @Override
    public void setReadTimeout(long timeout) {
        in.setTimeout(timeout);
    }

    @Override
    public void setWriteTimeout(long timeout) {
        out.setTimeout(timeout);
    }

    protected class Input extends InputStream {
        private final byte[] buffer;
        private int readIndex = 0;
        private int writeIndex = 0;

        private volatile long timeout;

        public Input(int inBufferSize) {
            buffer = new byte[inBufferSize];
        }

        @Override
        public int available() {
            synchronized (buffer) {
                return state.is(CLOSED) ? 0 : (writeIndex - readIndex + buffer.length) % buffer.length;
            }
        }

        @Override
        public int read() throws IOException {
            while (true) {
                if (state.is(CLOSED))
                    throw new EOFException("Connection is closed");
                if (!state.await(DATA_AVAILABLE | CLOSED | END_OF_INPUT, timeout))
                    throw new InterruptedIOException("Timeout");
                synchronized (buffer) {
                    if (readIndex == writeIndex) {
                        state.clear(DATA_AVAILABLE);
                        if (state.is(END_OF_INPUT))
                            return -1;
                        continue;
                    }
                    byte b = buffer[readIndex];
                    readIndex = (readIndex + 1) % buffer.length;
                    return b & 0xff;
                }
            }
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return super.read(b, off, Math.min(Math.max(available(), 1), len));
        }

        public void endOfStream() {
            state.set(DATA_AVAILABLE | END_OF_INPUT);
        }

        public void write(byte[] bytes) {
            write(bytes, 0, bytes.length);
        }

        public void write(byte[] bytes, int off, int len) {
            int skipped = 0;
            synchronized (buffer) {
                if (state.is(END_OF_INPUT))
                    throw new IllegalStateException("Should not have more data after end of stream");
                for (int i = 0; i < len; i++) {
                    byte b = bytes[i + off];
                    int w2 = (writeIndex + 1) % buffer.length;
                    if (w2 == readIndex) {
                        skipped = len - i;
                        break;
                    }
                    buffer[writeIndex] = b;
                    writeIndex = w2;
                }
            }
            if (skipped > 0)
                log.warning("Skipped " + skipped + " bytes from " +
                        BufferedConnection.this.toString() + " due to input buffer overflow");
            state.set(DATA_AVAILABLE);
        }

        @Override
        public void close() {
            BufferedConnection.this.close();
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

        // todo: implement write timeout
        private volatile long timeout;

        public Output(int outBufferSize) {
            buffer = new byte[outBufferSize];
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
                flushOutput(buffer, size);
                size = 0;
            }
        }

        @Override
        public void close() {
            BufferedConnection.this.close();
        }

        public void setTimeout(long timeout) {
            // todo: support timeout
            this.timeout = timeout;
        }
    }
}
