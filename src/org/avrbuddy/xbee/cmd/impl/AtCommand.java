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

import org.avrbuddy.xbee.api.XBeeAtFrame;
import org.avrbuddy.xbee.api.XBeeConnection;
import org.avrbuddy.xbee.api.XBeeFrameWithId;
import org.avrbuddy.xbee.api.XBeeUtil;
import org.avrbuddy.xbee.cmd.Command;
import org.avrbuddy.xbee.cmd.CommandContext;

import java.io.IOException;
import java.util.EnumSet;

/**
 * @author Roman Elizarov
 */
public class AtCommand extends Command {
    public AtCommand() {
        name = "<at>";
    }

    @Override
    public boolean hasName(String name) {
        return name.length() == 2;
    }

    @Override
    public EnumSet<Option> getOptions() {
        return EnumSet.of(Option.DESTINATION, Option.PARAMETER);
    }

    @Override
    public String getParameterDescription() {
        return "[<value>]";
    }

    @Override
    public String getCommandDescription() {
        return "send any AT command (node is local by default)";
    }

    @Override
    protected String executeImpl(CommandContext ctx) throws IOException {
        XBeeFrameWithId[] responses = ctx.conn.waitResponses(XBeeConnection.DEFAULT_TIMEOUT,
                ctx.conn.sendFramesWithId(XBeeAtFrame.newBuilder(
                            destination == null ? null : destination.resolveAddress(ctx))
                        .setAtCommand(name)
                        .setData(parameter == null ? new byte[0] : XBeeUtil.parseAtValue(name, parameter))));
        String result = "";
        if (responses[0] != null) {
            byte[] data = responses[0].getData();
            if (data.length > 0)
                result = " " + XBeeUtil.formatAtValue(name, data);
        }
        return ctx.conn.fmtStatus(responses) + result;
    }
}
