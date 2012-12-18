/*
 * Copyright (C) 2012 Roman Elizarov
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.avrbuddy.xbee;

import org.avrbuddy.log.Log;
import org.avrbuddy.serial.SerialConnection;
import org.avrbuddy.xbee.api.XBeeConnection;
import org.avrbuddy.xbee.cmd.CommandContext;
import org.avrbuddy.xbee.cmd.CommandParser;
import org.avrbuddy.xbee.cmd.impl.Help;
import org.avrbuddy.xbee.console.XBeeConsoleThread;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main XTools class.
 *
 * @author Roman Elizarov
 */
public class XTools {
    private static final Logger log = Log.getLogger(XTools.class);

    public static void main(String[] args) throws IOException {
        Log.init(XTools.class);
        if (args.length < 2) {
            log.info("Usage: xtools <port> <baud> [<command>]");
            log.info("Starts in console mode if no command is given on the command line.");
            Help.showHelp();
            return;
        }

        String port = args[0];
        int baud = Integer.parseInt(args[1]);
        String cmd = collect(args, 2, args.length).trim();

        try {
            new XTools(port, baud).go(cmd);
        } catch (Throwable t) {
            log.log(Level.SEVERE, "Failed", t);
        }
    }

    private static String collect(String[] args, int from, int to) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < to; i++) {
            if (i > from)
                sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }

    // ---------------------------------- instance ----------------------------------

    private CommandContext ctx;

    private XTools(String port, int baud) throws IOException {
        ctx = new CommandContext(XBeeConnection.open(SerialConnection.open(port, baud)));
    }

    private void go(String cmd) throws IOException {
        if (cmd.length() == 0) {
            // start console
            XBeeConsoleThread console = new XBeeConsoleThread(ctx);
            console.setDaemon(true);
            console.start();
        } else {
            // execute command
            CommandParser.parseCommand(cmd).execute(ctx);
            // close connection
            ctx.close();
        }
    }
}
