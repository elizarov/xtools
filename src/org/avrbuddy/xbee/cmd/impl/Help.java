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

package org.avrbuddy.xbee.cmd.impl;

import org.avrbuddy.xbee.api.XBeeAddress;
import org.avrbuddy.xbee.cmd.Command;
import org.avrbuddy.xbee.cmd.CommandContext;
import org.avrbuddy.xbee.cmd.CommandDestination;
import org.avrbuddy.xbee.cmd.CommandParser;

/**
 * @author Roman Elizarov
 */
public class Help extends Command {
    public Help() {
        setName("?");
    }

    public static void showHelp() {
        new Help().show();
    }

    @Override
    public String getCommandDescription() {
        return "print this help";
    }

    @Override
    protected String executeImpl(CommandContext ctx) {
        show();
        return null;
    }

    private void show() {
        logf("Available commands:");
        int n = CommandParser.PROTOTYPES.length;
        int m = 4;
        String[][] table = new String[n][m];
        int[] width = new int[m];
        for (int i = 0; i < n; i++) {
            Command prototype = CommandParser.PROTOTYPES[i];
            table[i][0] = prototype.getOptions().contains(Option.DESTINATION) ? "[<node>]" : "";
            table[i][1] = prototype.getName();
            table[i][2] = prototype.getParameterDescription();
            table[i][3] = prototype.getCommandDescription();
            for (int j = 0; j < m; j++)
                width[j] = Math.max(width[j], table[i][j].length());
        }
        String format = "  %-" + width[0] + "s %-" + width[1] + "s %-" + width[2] + "s -- %s";
        for (int i = 0; i < n; i++) {
            logf(format, (Object[]) table[i]);
        }
        logf("Where <node> is one of:");
        logf("  '%s'           -- local node", CommandDestination.NODE_LOCAL);
        logf("  '%s'           -- broadcast", XBeeAddress.BROADCAST_STRING);
        logf("  '%s'           -- coordinator node", XBeeAddress.COORDINATOR_STRING);
        logf("  '%s'<node-id>  -- a given node id (discovers by node id)", CommandDestination.NODE_ID_PREFIX);
        logf("  '['<hex>']'   -- a given serial number (8 bytes) and optional local address (2 bytes) in hex");
    }
    
    private void logf(String format, Object... args) {
        log.info(String.format(format, args));
    }
}
