package org.avrbuddy.xbee.console;

import org.avrbuddy.log.Log;
import org.avrbuddy.serial.SerialConnection;
import org.avrbuddy.xbee.api.XBeeConnection;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Roman Elizarov
 */
public class XBeeConsoleMain {
    private static final Logger log = Log.get(XBeeConsoleMain.class);

    public static void main(String[] args) throws IOException {
        Log.init(XBeeConsoleMain.class);
        if (args.length != 2) {
            log.log(Level.SEVERE, "Usage: " + XBeeConsoleMain.class.getName() + " <XBee-port> <baud>");
            return;
        }
        String port = args[0];
        int baud = Integer.parseInt(args[1]);
        XBeeConnection conn = XBeeConnection.open(SerialConnection.open(port, baud));
        new XBeeConsoleThread(conn).start();
    }
}
