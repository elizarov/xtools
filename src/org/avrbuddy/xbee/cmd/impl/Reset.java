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

import java.io.IOException;
import java.util.EnumSet;

/**
 * @author Roman Elizarov
 */
public class Reset extends Command {
    @Override
    public EnumSet<Option> getOptions() {
        return EnumSet.of(Option.DESTINATION);
    }

    @Override
    public String getCommandDescription() {
        return "reset node with D3 (node is local by default)";
    }

    @Override
    protected String executeImpl(CommandContext ctx) throws IOException {
        return ctx.conn.fmtStatus(ctx.conn.resetRemoteHost(
                destination == null ? null : destination.resolveAddress(ctx)));
    }
}
