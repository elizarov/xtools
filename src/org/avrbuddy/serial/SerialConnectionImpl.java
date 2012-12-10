package org.avrbuddy.serial;

import gnu.io.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;

/**
 * @author Roman Elizarov
 */
class SerialConnectionImpl extends SerialConnection implements SerialPortEventListener {
    private final String port;
    private final SerialPort serialPort;
    private final SerialInput in;
    private final SerialOutput out;

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
        in = new SerialInput(serialPort.getInputStream());
        out = new SerialOutput(serialPort.getOutputStream());
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
                in.dataAvailable();
                break;
            case SerialPortEvent.DSR:
                if (!event.getOldValue() && event.getNewValue())
                    in.connected();
                updateOutputEnabled();
                break;
            case SerialPortEvent.CTS:
                updateOutputEnabled();
                break;
        }
    }

    private void updateOutputEnabled() {
        out.setEnabled(serialPort.isDSR() || serialPort.isCTS());
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
    public void setPortConnectionAction(Runnable action) {
        in.setPortConnectionAction(action);
    }

    @Override
    public String toString() {
        return port;
    }
}
