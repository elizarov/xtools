package org.avrbuddy.xbee.console;

import org.avrbuddy.log.Log;
import org.avrbuddy.serial.SerialConnection;
import org.avrbuddy.xbee.api.XBeeConnection;
import org.avrbuddy.xbee.cmd.CommandContext;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Roman Elizarov
 */
public class XBeeConsole {
    private static final Logger log = Log.getLogger(XBeeConsole.class);

    public static void main(String[] args) throws IOException {
        Log.init(XBeeConsole.class);
        if (args.length != 2) {
            log.log(Level.SEVERE, "Usage: " + XBeeConsole.class.getName() + " <XBee-port> <baud>");
            return;
        }
        String port = args[0];
        int baud = Integer.parseInt(args[1]);
        XBeeConnection conn;
        try {
            conn = XBeeConnection.open(SerialConnection.open(port, baud));
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed", e);
            return;
        }
        XBeeConsoleThread console = new XBeeConsoleThread(new CommandContext(conn));
        console.setDaemon(true);
        console.start();
    }
}
