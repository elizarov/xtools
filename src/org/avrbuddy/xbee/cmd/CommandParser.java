package org.avrbuddy.xbee.cmd;

import org.avrbuddy.xbee.api.XBeeAddress;
import org.avrbuddy.xbee.cmd.impl.*;

/**
 * @author Roman Elizarov
 */
public class CommandParser {
    private CommandParser() {} // do not create

    private static final Command[] PROTOTYPES = {
            new Comment(),
            new Send(),
            new Dest(),
            new Reset(),
            new AtCommand(),
            new Discover(),
            new List(),
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

    public static void helpCommands() {
        // todo:
        System.err.printf("Available commands:%n");
        int m = 4;
        int[] width = new int[m];
        String[][] table = new String[PROTOTYPES.length][m];
        for (int i = 0; i < PROTOTYPES.length; i++) {
            Command prototype = PROTOTYPES[i];
            table[i][0] = prototype.getOptions().contains(Command.Option.DESTINATION) ? "[<node>]" : "";
            table[i][1] = prototype.getName();
            table[i][2] = prototype.getParameterDescription();
            table[i][3] = prototype.getCommandDescription();
            for (int j = 0; j < m; j++)
                width[j] = Math.max(width[j], table[i][j].length());
        }
        String format = "  %-" + width[0] + "s %-" + width[1] + "s %-" + width[2] + "s -- %s%n";
        for (int i = 0; i < PROTOTYPES.length; i++) {
            System.err.printf(format, (Object[]) table[i]);
        }
        System.err.printf("Where <node> is one of:%n");
        System.err.printf("  '%s'           -- local node%n", CommandDestination.NODE_LOCAL);
        System.err.printf("  '%s'           -- broadcast%n", XBeeAddress.BROADCAST_STRING);
        System.err.printf("  '%s'           -- coordinator node%n", XBeeAddress.COORDINATOR_STRING);
        System.err.printf("  '%s'<node-id>  -- a given node id (discovers by node id)%n", CommandDestination.NODE_ID_PREFIX);
        System.err.printf("  '['<hex>']'   -- a given serial number (8 bytes) and optional local address (2 bytes) in hex%n");
    }
}

