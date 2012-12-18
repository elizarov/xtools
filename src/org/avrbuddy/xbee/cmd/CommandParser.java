package org.avrbuddy.xbee.cmd;

import org.avrbuddy.xbee.cmd.impl.*;

/**
 * @author Roman Elizarov
 */
public class CommandParser {
    private CommandParser() {} // do not create

    public static final Command[] PROTOTYPES = {
            new Help(),
            new Comment(),
            new Send(),
            new Dest(),
            new Reset(),
            new AtCommand(),
            new Discover(),
            new List(),
            new Exit()
    };

    public static Command parseCommand(String line) {
        String[] s = line.trim().split("\\s", 2);
        CommandDestination destination = CommandDestination.parse(s[0]);
        if (destination != null) {
            if (s.length < 2)
                throw new InvalidCommandException("Command name is missing");
            s = s[1].split("\\s", 2);
        }
        String name = s[0];
        String parameter = s.length > 1 ? s[1] : null;
        for (Command prototype : PROTOTYPES) {
            if (prototype.hasName(name)) {
                Command cmd = prototype.clone();
                cmd.setName(name);
                cmd.setDestination(destination);
                cmd.setParameter(parameter);
                return cmd;
            }
        }
        throw new InvalidCommandException("Invalid command name '" + name + "'");
    }
}

