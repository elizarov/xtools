package org.avrbuddy.xbee.cmd.impl;

import org.avrbuddy.hex.HexUtil;
import org.avrbuddy.xbee.api.XBeeAddress;
import org.avrbuddy.xbee.api.XBeeConnection;
import org.avrbuddy.xbee.api.XBeeTxFrame;
import org.avrbuddy.xbee.cmd.Command;
import org.avrbuddy.xbee.cmd.CommandContext;
import org.avrbuddy.xbee.cmd.CommandDestination;
import org.avrbuddy.xbee.cmd.InvalidCommandException;

import java.io.IOException;
import java.util.EnumSet;

/**
 * @author Roman Elizarov
 */
public class Send extends Command {
    @Override
    public EnumSet<Option> getOptions() {
        return EnumSet.of(Option.DESTINATION, Option.PARAMETER);
    }

    @Override
    public String getParameterDescription() {
        return "<text>";
    }

    @Override
    public String getCommandDescription() {
        return "send text to node (broadcast by default)";
    }

    @Override
    public void validate() {
        if (destination == null)
            destination = new CommandDestination.Address(XBeeAddress.BROADCAST);
        if (parameter == null)
            throw new InvalidCommandException(name + ": text is missing");
    }

    @Override
    protected String executeImpl(CommandContext ctx) throws IOException {
        return ctx.conn.fmtStatus(ctx.conn.waitResponses(XBeeConnection.DEFAULT_TIMEOUT,
                ctx.conn.sendFramesWithId(XBeeTxFrame.newBuilder()
                        .setDestination(destination.resolveAddress(ctx))
                        .setData(HexUtil.parseAscii(parameter)))));
    }
}
