package org.avrbuddy.xbee.cmd.impl;

import org.avrbuddy.xbee.cmd.Command;
import org.avrbuddy.xbee.cmd.CommandContext;
import org.avrbuddy.xbee.cmd.CommandDestination;

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
