package org.avrbuddy.xbee.cmd.impl;

import org.avrbuddy.xbee.cmd.Command;
import org.avrbuddy.xbee.cmd.CommandContext;

/**
 * @author Roman Elizarov
 */
public class List extends Command {
    @Override
    public String getCommandDescription() {
        return "list discovered nodes";
    }

    @Override
    protected String executeImpl(CommandContext ctx) {
        ctx.discovery.list(this);
        return OK;
    }
}
