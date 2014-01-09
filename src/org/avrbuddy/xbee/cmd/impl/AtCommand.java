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

import org.avrbuddy.xbee.api.*;
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
        return EnumSet.of(Option.DEST, Option.ARG);
    }

    @Override
    public String getParameterDescription() {
        return "[<value>]";
    }

    @Override
    public String getCommandDescription() {
        return "Sends any AT command (node is local by default).";
    }

    @Override
    protected String invoke(final CommandContext ctx) throws IOException {
        XBeeAddress destAddress = destination == null ? null : destination.resolveAddress(ctx);
        final XBeeFrameWithId[] frames = ctx.conn.buildFramesWithId(
                XBeeAtFrame.newBuilder(destAddress)
                    .setAtCommand(name)
                    .setData(arg == null ? new byte[0] : XBeeUtil.parseAtValue(name, arg)));
        if (XBeeAddress.BROADCAST.equals(destAddress)) {
            // special logic for broadcast
            XBeeTerminatingFrameListener<XBeeAtResponseFrame> listener = new XBeeTerminatingFrameListener<XBeeAtResponseFrame>() {
                private boolean terminated;

                @Override
                public boolean isTerminated() {
                    return terminated;
                }

                @Override
                public void frameReceived(XBeeAtResponseFrame frame) {
                    if (frame.isResponseFor(frames[0]))
                        log.info(frame.getSource() + " " + resultString(frame));
                }

                @Override
                public void connectionClosed() {
                    terminated = true;
                }
            };
            ctx.conn.sendFramesAndWaitWithListener(XBeeConnection.BROADCAST_TIMEOUT, XBeeAtResponseFrame.class, listener, frames);
            return OK;
        } else {
            // unicast
            XBeeFrameWithId[] responses = ctx.conn.sendFramesWithIdAndWaitResponses(XBeeConnection.DEFAULT_TIMEOUT, frames);
            XBeeUtil.checkStatus(responses);
            return resultString(responses[0]);
        }
    }

    private String resultString(XBeeFrameWithId response) {
        if (response == null)
            return XBeeUtil.formatStatus(XBeeUtil.STATUS_TIMEOUT);
        StringBuilder result = new StringBuilder(XBeeUtil.formatStatus(response.getStatus()));
        byte[] data = response.getData();
        if (data.length > 0)
            result.append(" ").append(XBeeUtil.formatAtValue(name, data));
        return result.toString();
    }
}
