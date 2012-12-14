package org.avrbuddy.serial;

import gnu.io.*;
import org.avrbuddy.util.State;

import java.io.*;
import java.util.TooManyListenersException;
import java.util.logging.Level;

/**
 * @author Roman Elizarov
 */
class SerialConnectionImpl extends SerialConnection implements SerialPortEventListener {
    private static final int CLOSED = 1;
    private static final int DATA_AVAILABLE = 2;
    private static final int OUTPUT_ENABLED = 4;
    private static final int NOTIFY_CONNECTED = 8;

    private final String port;
    private final SerialPort serialPort;
    private final Input in;
    private final Output out;
    private final State state = new State();

    private volatile Runnable onConnected;

    SerialConnectionImpl(String port, int baud) throws IOException {
        this.port = port;
        try {
            CommPort commPort = CommPortIdentifier.getPortIdentifier(port).open(this.getClass().getName(), 2000);
            if (!(commPort instanceof SerialPort)) {
                commPort.close();
                throw new IOException("Port " + port + " is not a serial port");
            }
            serialPort = (SerialPort) commPort;
        } catch (NoSuchPortException e) {
            throw new IOException("Port " + port + " is not found");
        } catch (PortInUseException e) {
            throw new IOException("Port " + port + " is currently in use");
        }
        try {
            serialPort.setSerialPortParams(baud, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        } catch (UnsupportedCommOperationException e) {
            serialPort.close();
            throw new IOException("Port " + port + " cannot be configured for " + baud + " 8N1");
        }
        in = new Input(serialPort.getInputStream());
        out = new Output(serialPort.getOutputStream());
        serialPort.notifyOnDataAvailable(true);
        serialPort.notifyOnDSR(true);
        serialPort.notifyOnCTS(true);
        try {
            serialPort.addEventListener(this);
        } catch (TooManyListenersException e) {
            throw new IOException(e);
        }
        updateOutputEnabled();
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        switch (event.getEventType()) {
            case SerialPortEvent.DATA_AVAILABLE:
                state.set(DATA_AVAILABLE);
                break;
            case SerialPortEvent.DSR:
                if (!event.getOldValue() && event.getNewValue())
                    state.set(NOTIFY_CONNECTED);
                updateOutputEnabled();
                break;
            case SerialPortEvent.CTS:
                updateOutputEnabled();
                break;
        }
    }

    private void updateOutputEnabled() {
        if (serialPort.isDSR() || serialPort.isCTS())
            state.set(OUTPUT_ENABLED);
        else
            state.clear(OUTPUT_ENABLED);
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
        in.closeImpl();
        out.closeImpl();
        serialPort.close();
    }

    @Override
    public void resetHost() throws IOException {
        serialPort.setDTR(false);
        serialPort.setDTR(true);
    }

    @Override
    public void drainInput() throws IOException {
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

    @Override
    public void setHardwareFlowControl(int mode) throws IOException {
        try {
            serialPort.setFlowControlMode(
                    ((mode & SerialConnection.FLOW_CONTROL_IN) == 0  ? 0 : SerialPort.FLOWCONTROL_RTSCTS_IN) |
                    ((mode & SerialConnection.FLOW_CONTROL_OUT) == 0 ? 0 : SerialPort.FLOWCONTROL_RTSCTS_OUT));
        } catch (UnsupportedCommOperationException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void setOnConnected(Runnable action) {
        onConnected = action;
    }

    void checkNotifyConnected() {
        if (state.clear(NOTIFY_CONNECTED)) {
            Runnable onConnected = this.onConnected;
            if (onConnected != null)
                onConnected.run();
        }
    }

    @Override
    public String toString() {
        return port;
    }

    class Input extends InputStream {
        private final InputStream in;
        private volatile long timeout;

        public Input(InputStream in) throws IOException {
            this.in = in;
        }

        @Override
        public int available() throws IOException {
            return in.available();
        }

        @Override
        public int read() throws IOException {
            while (true) {
                if (state.is(CLOSED))
                    throw new EOFException("Port is closed");
                checkNotifyConnected();
                if (in.available() != 0)
                    return in.read();
                state.await(DATA_AVAILABLE | CLOSED, timeout);
                state.clear(DATA_AVAILABLE);
            }
        }

        @Override
        public void close() {
            SerialConnectionImpl.this.close();
        }

        void closeImpl() {
            try {
                in.close();
            } catch (IOException e) {
                log.log(Level.SEVERE, "Failed to close input", e);
            }
        }

        public void drain() throws IOException {
            while (in.available() > 0)
                in.read();
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }
    }

    class Output extends BufferedOutputStream {
        private volatile long timeout;

        public Output(OutputStream out) {
            super(out);
        }

        @Override
        public void write(int b) throws IOException {
            if (waitEnabled())
                super.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (waitEnabled())
                super.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            if (waitEnabled())
                super.flush();
        }

        private boolean waitEnabled() throws IOException {
            state.await(OUTPUT_ENABLED | CLOSED, timeout);
            if (state.is(CLOSED))
                throw new EOFException("Port is closed");
            return state.is(OUTPUT_ENABLED);
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }

        @Override
        public void close() {
            SerialConnectionImpl.this.close();
        }

        void closeImpl() {
            try {
                out.close();
            } catch (IOException e) {
                log.log(Level.SEVERE, "Failed to close output", e);
            }
        }
    }
}
