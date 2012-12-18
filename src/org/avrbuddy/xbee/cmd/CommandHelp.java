package org.avrbuddy.xbee.cmd;

import org.avrbuddy.log.Log;
import org.avrbuddy.xbee.cmd.impl.Help;

import java.io.IOException;

/**
 * @author Roman Elizarov
 */
public class CommandHelp {
    public static void main(String[] args) throws IOException {
        Log.init(CommandHelp.class);
        new Help().execute(null);
    }
}
