package org.avrbuddy.xbee.cmd.impl;

import org.avrbuddy.xbee.api.XBeeAddress;
import org.avrbuddy.xbee.cmd.Command;
import org.avrbuddy.xbee.cmd.CommandContext;
import org.avrbuddy.xbee.cmd.CommandDestination;
import org.avrbuddy.xbee.cmd.CommandParser;

import java.io.IOException;

/**
 * @author Roman Elizarov
 */
public class Help extends Command {
    public Help() {
        setName("?");
    }

    @Override
    public String getCommandDescription() {
        return "print this help";
    }

    @Override
    protected String executeImpl(CommandContext ctx) throws IOException {
        logf("Available commands:");
        int n = CommandParser.PROTOTYPES.length;
        int m = 4;
        String[][] table = new String[n][m];
        int[] width = new int[m];
        for (int i = 0; i < n; i++) {
            Command prototype = CommandParser.PROTOTYPES[i];
            table[i][0] = prototype.getOptions().contains(Option.DESTINATION) ? "[<node>]" : "";
            table[i][1] = prototype.getName();
            table[i][2] = prototype.getParameterDescription();
            table[i][3] = prototype.getCommandDescription();
            for (int j = 0; j < m; j++)
                width[j] = Math.max(width[j], table[i][j].length());
        }
        String format = "  %-" + width[0] + "s %-" + width[1] + "s %-" + width[2] + "s -- %s";
        for (int i = 0; i < n; i++) {
            logf(format, (Object[]) table[i]);
        }
        logf("Where <node> is one of:");
        logf("  '%s'           -- local node", CommandDestination.NODE_LOCAL);
        logf("  '%s'           -- broadcast", XBeeAddress.BROADCAST_STRING);
        logf("  '%s'           -- coordinator node", XBeeAddress.COORDINATOR_STRING);
        logf("  '%s'<node-id>  -- a given node id (discovers by node id)", CommandDestination.NODE_ID_PREFIX);
        logf("  '['<hex>']'   -- a given serial number (8 bytes) and optional local address (2 bytes) in hex");
        return null;
    }
    
    private void logf(String format, Object... args) {
        log.info(String.format(format, args));
    }
}
