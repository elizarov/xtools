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
import org.avrbuddy.xbee.cmd.CommandContext;
import org.avrbuddy.xbee.cmd.CommandDestination;

import java.io.IOException;
import java.util.EnumSet;

/**
 * @author Roman Elizarov
 */
public class Dest extends Command {
    private CommandDestination target;

    @Override
    public EnumSet<Option> getOptions() {
        return EnumSet.of(Option.DEST, Option.DEST_REQUIRED, Option.ARG);
    }

    @Override
    public String getParameterDescription() {
        return "[<target>]";
    }

    @Override
    public String getCommandDescription() {
        return "Changes destination node with DH,DL (target is local by default).";
    }

    @Override
    public void validate(CommandContext ctx) {
        super.validate(ctx);
        target = arg == null ? CommandDestination.LOCAL : CommandDestination.parse(arg);
    }

    @Override
    protected String invoke(CommandContext ctx) throws IOException {
        int status = ctx.conn.changeRemoteDestination(
                destination.resolveAddress(ctx),
                target.resolveAddress(ctx));
        return ctx.conn.fmtStatus(status);
    }
}

