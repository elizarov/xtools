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
        return EnumSet.of(Option.DESTINATION, Option.PARAMETER);
    }

    @Override
    public String getParameterDescription() {
        return "[<target-node>]";
    }

    @Override
    public String getCommandDescription() {
        return "change destination node with DH,DL (nodes are local by default)";
    }

    @Override
    public void validate() {
        target = parameter == null ? CommandDestination.LOCAL : CommandDestination.parse(parameter);
    }

    @Override
    protected String executeImpl(CommandContext ctx) throws IOException {
        int status = ctx.conn.changeRemoteDestination(
                destination == null ? null : destination.resolveAddress(ctx),
                target.resolveAddress(ctx));
        return ctx.conn.fmtStatus(status);
    }
}

