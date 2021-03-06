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
import org.avrbuddy.xbee.cmd.CommandConnection;
import org.avrbuddy.xbee.cmd.CommandContext;
import org.avrbuddy.xbee.link.XBeeLink;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;

/**
 * @author Roman Elizarov
 */
public class Link extends Command {
    private CommandConnection conn;

    @Override
    public EnumSet<Option> getOptions() {
        return EnumSet.of(Option.DEST, Option.ARG);
    }

    @Override
    public String getCommandDescription() {
        return "Links remote node input/output to a specified connection (to console by default).";
    }

    @Override
    public String getParameterDescription() {
        return "[<conn>]";
    }

    @Override
    public String getMoreHelp() {
        return
            "When <node> is not specified, then input from all nodes is linked to <conn> and transmissions are broadcast.\n" +
            getConnHelpString("Link to");
    }

    public static String getConnHelpString(String op) {
        ArrayList<String[]> table = new ArrayList<String[]>();
        FmtUtil.line(table, "<node>", FmtUtil.SEP, op + " other remote node.");
        FmtUtil.line(table, "<port> [<baud>]", FmtUtil.SEP, op + " serial port.");
        return "Where <conn> is one of:\n" + FmtUtil.formatTable(table);
    }

    @Override
    public void validate(CommandContext ctx) {
        super.validate(ctx);
        conn = CommandConnection.parse(arg, ctx.options);
    }

    @Override
    protected String invoke(CommandContext ctx) throws IOException {
        XBeeAddress address = destination == null ? XBeeAddress.BROADCAST : destination.resolveAddress(ctx);
        XBeeLink link = new XBeeLink(ctx, address, conn);
        try {
            link.start();
        } catch (IOException e) {
            link.close();
            throw e;
        }
        return OK;
    }
}
