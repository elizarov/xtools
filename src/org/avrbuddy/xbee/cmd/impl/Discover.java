package org.avrbuddy.xbee.cmd.impl;

import org.avrbuddy.xbee.api.XBeeAddress;
import org.avrbuddy.xbee.cmd.Command;
import org.avrbuddy.xbee.cmd.CommandContext;
import org.avrbuddy.xbee.cmd.CommandDestination;
import org.avrbuddy.xbee.discover.XBeeNode;

import java.io.IOException;
import java.util.EnumSet;

/**
 * @author Roman Elizarov
 */
public class Discover extends Command {
    @Override
    public EnumSet<Option> getOptions() {
        return EnumSet.of(Option.DESTINATION);
    }

    @Override
    public String getCommandDescription() {
        return "discover node (all nodes by default)";
    }

    @Override
    public void validate() {
        if (destination == null)
            destination = new CommandDestination.Address(XBeeAddress.BROADCAST);
    }

    @Override
    protected String executeImpl(CommandContext ctx) throws IOException {
        if (destination.isBroadcast()) {
            int status = ctx.discovery.discoverAllNodes(this);
            return ctx.conn.fmtStatus(status);
        } else {
            XBeeNode node = destination.resolveNode(ctx);
            return node == null ? FAILED : OK + " " + node;
        }
    }
}
