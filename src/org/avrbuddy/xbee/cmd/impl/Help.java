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

import org.avrbuddy.util.FmtUtil;
import org.avrbuddy.xbee.api.XBeeAddress;
import org.avrbuddy.xbee.cmd.Command;
import org.avrbuddy.xbee.cmd.CommandContext;
import org.avrbuddy.xbee.cmd.CommandDestination;
import org.avrbuddy.xbee.cmd.CommandParser;
import org.avrbuddy.xbee.discover.XBeeNode;

import java.util.ArrayList;
import java.util.EnumSet;

/**
 * @author Roman Elizarov
 */
public class Help extends Command {
    private static final String HELP_COMMAND = "?";

    public Help() {
        setName(HELP_COMMAND);
    }

    public static void showHelp() {
        new Help().showAllCommands();
    }

    @Override
    public EnumSet<Option> getOptions() {
        return EnumSet.of(Option.ARG);
    }

    @Override
    public String getCommandDescription() {
        return "Prints this help.";
    }

    @Override
    public String getParameterDescription() {
        return "[<topic>]";
    }

    @Override
    public String getHelpName() {
        return "'" + HELP_COMMAND + "'";
    }

    @Override
    protected String invoke(CommandContext ctx) {
        if (arg != null)
            showTopic(arg);
        else
            showAllCommands();
        return null;
    }

    private void showTopic(String topic) {
        Command cmd = CommandParser.parseCommand(topic);
        info(cmd.getName() + ": " + cmd.getCommandDescription());
        info("Use: " + cmd.getDestinationDescription() + " " + cmd.getHelpName() + " " + cmd.getParameterDescription());
        String moreHelp = cmd.getMoreHelp();
        if (moreHelp != null)
            info(moreHelp);
    }

    private void showAllCommands() {
        ArrayList<String[]> table = new ArrayList<String[]>();

        info("Available commands:");
        for (Command prototype : CommandParser.PROTOTYPES) {
            FmtUtil.line(table,
                    prototype.getDestinationDescription(),
                    prototype.getHelpName(),
                    prototype.getParameterDescription(),
                    FmtUtil.SEP,
                    prototype.getCommandDescription() +
                            (prototype.getMoreHelp() != null ?
                                    "\nType '" + HELP_COMMAND + " " + prototype.getName() + "' for more details." : "")
            );
        }
        printTable(table);

        info("Where <node> is one of:");
        table.clear();
        FmtUtil.line(table, "'" + CommandDestination.LOCAL_STRING + "'", FmtUtil.SEP, "local node;");
        FmtUtil.line(table, "'" + CommandDestination.BROADCAST + "'", FmtUtil.SEP, "broadcast;");
        FmtUtil.line(table, "'" + XBeeAddress.COORDINATOR_STRING + "'", FmtUtil.SEP, "coordinator node;");
        FmtUtil.line(table, "'" + XBeeNode.NODE_ID_PREFIX + "'<node-id>", FmtUtil.SEP,
                "a given node id (discovers by node id);");
        FmtUtil.line(table, "'['<hex>']'", FmtUtil.SEP,
                "a given serial number (8 bytes) and optional local address (2 bytes) in hex.");
        printTable(table);
    }

    private void printTable(ArrayList<String[]> table) {
        info(FmtUtil.formatTable(table));
    }

    private void info(String msg) {
        String[] ss = msg.split("\n");
        for (String s : ss)
            log.info(s);
    }
}
