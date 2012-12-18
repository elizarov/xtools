package org.avrbuddy.xbee.cmd.impl;

import org.avrbuddy.xbee.cmd.Command;
import org.avrbuddy.xbee.cmd.CommandContext;

import java.io.IOException;

/**
 * @author Roman Elizarov
 */
public class Exit extends Command {
    @Override
    public String getCommandDescription() {
        return "exit the process";
    }

    @Override
    protected String executeImpl(CommandContext ctx) throws IOException {
        System.exit(0);
        return null;
    }
}
