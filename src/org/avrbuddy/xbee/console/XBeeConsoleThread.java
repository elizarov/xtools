package org.avrbuddy.xbee.console;

import org.avrbuddy.log.Log;
import org.avrbuddy.xbee.api.*;
import org.avrbuddy.xbee.cmd.CommandProcessor;
import org.avrbuddy.xbee.discover.XBeeNodeDiscovery;
import sun.util.logging.PlatformLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Roman Elizarov
 */
class XBeeConsoleThread extends Thread {
    private static final Logger log = Log.getLogger(XBeeConsoleThread.class);

    private final XBeeNodeDiscovery discovery;
    private final CommandProcessor processor;

    public XBeeConsoleThread(XBeeConnection conn) {
        super(XBeeConsoleThread.class.getName());
        discovery = new XBeeNodeDiscovery(conn);
        processor = new CommandProcessor(conn, discovery);
    }

    @Override
    public void run() {
        try {
            discovery.discoverAllNodes();
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                System.out.print("> ");
                String line = in.readLine();
                if (line == null) {
                    log.info("End of input stream");
                    return;
                }
                String result = processor.processCommand(line);
                if (result != null)
                    System.out.println(result);
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Error while reading from console", e);
        } catch (InterruptedException e) {
            log.info("Thread interrupted");
        }
    }
}
