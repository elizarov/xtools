package org.avrbuddy.xbee.console;

import org.avrbuddy.log.Log;
import org.avrbuddy.xbee.cmd.CommandContext;
import org.avrbuddy.xbee.cmd.CommandParser;
import org.avrbuddy.xbee.cmd.InvalidCommandException;

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

    private final CommandContext ctx;

    public XBeeConsoleThread(CommandContext ctx) {
        super(XBeeConsoleThread.class.getName());
        this.ctx = ctx;
    }

    @Override
    public void run() {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String line;
            try {
                line = in.readLine();
            } catch (IOException e) {
                log.log(Level.SEVERE, "Error while reading from console", e);
                return;
            }
            if (line == null) {
                log.info("End of input stream");
                return;
            }
            try {
                CommandParser.parseCommand(line).execute(ctx);
            } catch (InvalidCommandException e) {
                log.log(Level.WARNING, e.getMessage());
                CommandParser.helpCommands();
            } catch (IllegalArgumentException e) {
                log.log(Level.WARNING, e.getMessage());
            } catch (IOException e) {
                log.log(Level.SEVERE, "Failed", e);
            }
        }
    }
}
