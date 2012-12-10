package org.avrbuddy.serial;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

/**
 * @author Roman Elizarov
 */
public abstract class SerialConnection implements Closeable {
    public abstract InputStream getInput();
    public abstract OutputStream getOutput();
    public abstract void close();
    public abstract void resetHost() throws IOException;
    public abstract void drainInput() throws IOException;
    public abstract void setReadTimeout(long timeout) throws IOException;

    public void setPortConnectionAction(Runnable action) {}

    public static SerialConnection open(String port, int baud) throws IOException {
        System.err.println("Opening serial port " + port + " at " + baud);
        return new SerialConnectionImpl(port, baud);
    }
}
