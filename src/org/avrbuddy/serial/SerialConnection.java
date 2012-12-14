package org.avrbuddy.serial;

import org.avrbuddy.log.Log;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

/**
 * @author Roman Elizarov
 */
public abstract class SerialConnection implements Closeable {
    protected static final Logger log = Log.getLogger(SerialConnection.class);

    public static final int FLOW_CONTROL_IN = 1;
    public static final int FLOW_CONTROL_OUT = 2;

    public abstract InputStream getInput();
    public abstract OutputStream getOutput();
    public abstract void close();
    public abstract void resetHost() throws IOException;
    public abstract void drainInput() throws IOException;
    public abstract void setReadTimeout(long timeout) throws IOException;
    public abstract void setWriteTimeout(long timeout) throws IOException;

    public void setHardwareFlowControl(int mode) throws IOException {}

    public void setOnConnected(Runnable action) {}

    public static SerialConnection open(String port, int baud) throws IOException {
        log.info("Opening serial port " + port + " at " + baud);
        return new SerialConnectionImpl(port, baud);
    }
}
