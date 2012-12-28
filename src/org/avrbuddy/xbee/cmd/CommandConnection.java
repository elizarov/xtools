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

import org.avrbuddy.conn.Connection;
import org.avrbuddy.conn.ConnectionOptions;
import org.avrbuddy.conn.ConsoleConnection;
import org.avrbuddy.conn.SerialConnection;
import org.avrbuddy.util.WrongFormatException;

import java.io.IOException;

/**
 * @author Roman Elizarov
 */
public abstract class CommandConnection {
    public static CommandConnection parse(String spec, ConnectionOptions options) {
        if (spec == null || spec.trim().isEmpty())
            return new Console(options);
        String[] s = spec.trim().split("\\s", 2);
        String port = s[0];
        // other node
        CommandDestination remote = CommandDestination.parse(spec);
        if (remote != null)
            return new Remote(remote, options);
        // serial port
        if (s.length > 1) {
            String baudStr = s[1];
            try {
                options = new ConnectionOptions(Integer.parseInt(baudStr));
            } catch (NumberFormatException e) {
                throw new WrongFormatException("Baud is expected as an argument, but '" + baudStr + "' is found");
            }
        }
        return new Serial(port, options);
    }

    // -------------------------- instance --------------------------

    protected final ConnectionOptions options;

    private CommandConnection(ConnectionOptions options) {
        this.options = options;
    }

    public abstract Connection openConnection(CommandContext ctx) throws IOException;

    private static class Console extends CommandConnection {
        private Console(ConnectionOptions options) {
            super(options);
        }

        @Override
        public Connection openConnection(CommandContext ctx) {
            return ConsoleConnection.openRemoteConsole(options);
        }

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Console;
        }
    }

    private static class Remote extends CommandConnection {
        private final CommandDestination remote;

        private Remote(CommandDestination remote, ConnectionOptions options) {
            super(options);
            this.remote = remote;
        }

        @Override
        public Connection openConnection(CommandContext ctx) throws IOException {
            return ctx.conn.openTunnel(remote.resolveAddress(ctx));
        }

        @Override
        public int hashCode() {
            return remote.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof Remote && remote.equals(((Remote) o).remote);
        }
    }

    private static class Serial extends CommandConnection {
        private final String port;

        private Serial(String port, ConnectionOptions options) {
            super(options);
            this.port = port;
        }

        @Override
        public Connection openConnection(CommandContext ctx) throws IOException {
            return SerialConnection.open(port, options);
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof Serial && port.equals(((Serial) o).port);

        }

        @Override
        public int hashCode() {
            return port.hashCode();
        }
    }
}
