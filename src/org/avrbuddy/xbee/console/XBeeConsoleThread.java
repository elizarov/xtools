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

package org.avrbuddy.xbee.console;

import org.avrbuddy.conn.ConsoleConnection;
import org.avrbuddy.log.LoggedThread;
import org.avrbuddy.xbee.cmd.CommandContext;
import org.avrbuddy.xbee.cmd.CommandParser;
import org.avrbuddy.xbee.cmd.impl.Help;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;

/**
 * @author Roman Elizarov
 */
public class XBeeConsoleThread extends LoggedThread {
    private final CommandContext ctx;

    public XBeeConsoleThread(CommandContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void run() {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(ConsoleConnection.getCommandConsole().getInput()));
        while (true) {
            if (!ConsoleConnection.hasRemoteConsoles())
                System.err.print("> ");
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
            boolean ok = false;
            try {
                ok = CommandParser.parseCommand(line).execute(ctx);
            } catch (IllegalArgumentException e) {
                log.log(Level.WARNING, null, e);
            }
            if (!ok)
                log.info("Type '" + new Help() + "' for help.");
        }
    }
}
