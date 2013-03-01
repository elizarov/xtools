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

import org.avrbuddy.hex.HexUtil;
import org.avrbuddy.util.WrongFormatException;
import org.avrbuddy.xbee.api.XBeeAddress;
import org.avrbuddy.xbee.api.XBeeConnection;
import org.avrbuddy.xbee.api.XBeeTxFrame;
import org.avrbuddy.xbee.api.XBeeUtil;
import org.avrbuddy.xbee.cmd.Command;
import org.avrbuddy.xbee.cmd.CommandContext;

import java.io.IOException;
import java.util.EnumSet;

/**
 * @author Roman Elizarov
 */
public class Send extends Command {
    @Override
    public EnumSet<Option> getOptions() {
        return EnumSet.of(Option.DEST, Option.ARG);
    }

    @Override
    public String getParameterDescription() {
        return "<text>";
    }

    @Override
    public String getCommandDescription() {
        return "Sends text to node (broadcast by default).";
    }

    @Override
    public void validate(CommandContext ctx) {
        super.validate(ctx);
        if (arg == null)
            throw new WrongFormatException(name + ": text is missing");
    }

    @Override
    protected String invoke(CommandContext ctx) throws IOException {
        XBeeUtil.checkStatus(ctx.conn.waitResponses(XBeeConnection.DEFAULT_TIMEOUT,
                ctx.conn.sendFramesWithId(XBeeTxFrame.newBuilder()
                        .setDestination(destination == null ? XBeeAddress.BROADCAST : destination.resolveAddress(ctx))
                        .setData(HexUtil.parseAscii(arg)))));
        return OK;
    }
}
