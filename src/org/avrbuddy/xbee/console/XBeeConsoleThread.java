package org.avrbuddy.xbee.console;

import org.avrbuddy.xbee.api.*;
import org.avrbuddy.xbee.cmd.CommandProcessor;
import org.avrbuddy.xbee.discover.XBeeNodeDiscovery;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author Roman Elizarov
 */
class XBeeConsoleThread extends Thread {
    private final XBeeConnection conn;
    private final XBeeNodeDiscovery discovery;
    private final CommandProcessor processor;

    public XBeeConsoleThread(XBeeConnection conn) {
        super(XBeeConsoleThread.class.getName());
        this.conn = conn;
        discovery = new XBeeNodeDiscovery(conn);
        processor = new CommandProcessor(conn, discovery);
    }

    @Override
    public void run() {
        try {
            discovery.discover();
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            String line;
            while ((line = in.readLine()) != null)
                processor.processCommand(line);
        } catch (IOException e) {
            System.err.println("Error while reading from console");
            e.printStackTrace();
        } catch (InterruptedException e) {
            // ignored - quit
        }
    }
}
