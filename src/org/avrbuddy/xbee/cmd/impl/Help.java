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
import org.avrbuddy.xbee.discover.XBeeNode;

import java.util.ArrayList;

/**
 * @author Roman Elizarov
 */
public class Help extends Command {
    private static final String SEP = "-";
    private static final String HELP_COMMAND = "?";

    public Help() {
        setName(HELP_COMMAND);
    }

    public static void showHelp() {
        new Help().show();
    }

    @Override
    public String getCommandDescription() {
        return "print this help";
    }

    @Override
    public String getHelpName() {
        return "'" + HELP_COMMAND + "'";
    }

    @Override
    protected String invoke(CommandContext ctx) {
        show();
        return null;
    }

    private void show() {
        ArrayList<String[]> table = new ArrayList<String[]>();

        info("Available commands:");
        for (Command prototype : CommandParser.PROTOTYPES) {
            table.add(str(
                    prototype.getDestinationDescription(),
                    prototype.getHelpName(),
                    prototype.getParameterDescription(),
                    SEP,
                    prototype.getCommandDescription()
            ));
        }
        printTable(table);

        info("Where <conn> is one of:");
        table.clear();
        table.add(str("<node>", SEP, "link to other remote node"));
        table.add(str("<port> [<baud>]", SEP, "link to serial port"));
        printTable(table);

        info("Where <node> is one of:");
        table.clear();
        table.add(str("'" + CommandDestination.LOCAL_STRING + "'", SEP, "local node"));
        table.add(str("'" + CommandDestination.BROADCAST + "'", SEP, "broadcast"));
        table.add(str("'" + XBeeAddress.COORDINATOR_STRING + "'", SEP, "coordinator node"));
        table.add(str("'" + XBeeNode.NODE_ID_PREFIX + "'<node-id>", SEP,
                "a given node id (discovers by node id)"));
        table.add(str("'['<hex>']'", SEP,
                "a given serial number (8 bytes) and optional local address (2 bytes) in hex"));
        printTable(table);

        info("Where <memop> is <memtype>:<memcmd>:<filename>[:<format>] and");
        info("  <memtype> is 'f' or 'e'; <memcmd> is 'r', 'w', or 'v'; and the only supported <format> in 'i'");
    }

    private static String[] str(String... s) {
        return s;
    }

    private void printTable(ArrayList<String[]> table) {
        int m = table.get(0).length;
        int[] width = new int[m];
        for (String[] row : table)
            for (int j = 0; j < m; j++)
                width[j] = Math.max(width[j], row[j].length());
        StringBuilder sb = new StringBuilder(" ");
        for (int j = 0; j < m; j++)
            sb.append(" %-").append(width[j]).append("s");
        String format = sb.toString();
        for (String[] row : table)
            info(String.format(format, (Object[]) row));
    }

    private void info(String msg) {
        log.info(msg);
    }
}
