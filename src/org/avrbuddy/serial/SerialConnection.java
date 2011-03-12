package org.avrbuddy.serial;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Roman Elizarov
 */
public abstract class SerialConnection implements Closeable {
    public abstract InputStream getInput();
    public abstract OutputStream getOutput();
    public abstract void close();
    public void drainInput() {}

    public static SerialConnection open(String port, int baud) throws IOException {
        return new SerialConnectionImpl(port, baud);
    }

}