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

import org.avrbuddy.xbee.cmd.Command;
import org.avrbuddy.xbee.cmd.CommandConnection;
import org.avrbuddy.xbee.cmd.CommandContext;
import org.avrbuddy.xbee.link.XBeeLink;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;

/**
 * @author Roman Elizarov
 */
public class Unlink extends Command {
    private CommandConnection conn;

    @Override
    public EnumSet<Option> getOptions() {
        return EnumSet.of(Option.DEST, Option.ARG);
    }

    @Override
    public String getParameterDescription() {
        return "[<conn>]";
    }

    @Override
    public String getCommandDescription() {
        return "unlink remote connection (all connections to console by default)";
    }

    @Override
    public void validate(CommandContext ctx) {
        super.validate(ctx);
        conn = CommandConnection.parse(arg, ctx.options);
    }

    @Override
    protected String invoke(CommandContext ctx) throws IOException {
        List<XBeeLink> links = destination == null ? ctx.getLinks() :
                ctx.getLinks(destination.resolveAddress(ctx));
        int cnt = 0;
        for (XBeeLink link : links)
            if (conn.equals(link.getLinkConnection())) {
                link.close();
                cnt++;
            }
        return OK + ": Closed " + cnt + " " + (cnt == 1 ? "link" : "links");
    }
}
