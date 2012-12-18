/*
 * Copyright (C) 2012 Roman Elizarov
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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

