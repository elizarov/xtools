package org.avrbuddy.xbee.cmd.impl;

import org.avrbuddy.xbee.cmd.Command;
import org.avrbuddy.xbee.cmd.CommandContext;

import java.io.IOException;
import java.util.EnumSet;

/**
 * @author Roman Elizarov
 */
public class Comment extends Command {
    public static final String COMMENT_PREFIX = "#";

    public Comment() {
        name = COMMENT_PREFIX;
    }

    @Override
    public EnumSet<Option> getOptions() {
        return EnumSet.of(Option.PARAMETER);
    }

    @Override
    public String getCommandDescription() {
        return "use to comment a line (does nothing)";
    }

    @Override
    public boolean hasName(String name) {
        return name.isEmpty() || name.startsWith(COMMENT_PREFIX);
    }

    @Override
    protected String executeImpl(CommandContext ctx) throws IOException {
        return null; // does nothing
    }
}
