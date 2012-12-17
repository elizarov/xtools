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
