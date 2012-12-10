package org.avrbuddy.xbee.console;

import org.avrbuddy.serial.SerialConnection;
import org.avrbuddy.xbee.api.XBeeConnection;

import java.io.IOException;

/**
 * @author Roman Elizarov
 */
public class XBeeConsoleMain {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: " + XBeeConsoleMain.class.getName() + " <XBee-port> <baud>");
            return;
        }
        String port = args[0];
        int baud = Integer.parseInt(args[1]);
        XBeeConnection conn = XBeeConnection.open(SerialConnection.open(port, baud));
        new XBeeConsoleParser(conn).start();
    }
}
