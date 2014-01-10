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

package org.avrbuddy.conn;

import org.avrbuddy.log.LoggedThread;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

/**
 * @author Roman Elizarov
 */
public class ConsoleConnection extends BufferedConnection {
    private static final char CMD_ESCAPE_CHAR = '^';

    private static final int BUFFER_SIZE = 1024;

    private static volatile ConsoleConnection commandConsole;
    private static final List<ConsoleConnection> remoteConsoles = new CopyOnWriteArrayList<ConsoleConnection>();

    public static ConsoleConnection openRemoteConsole(ConnectionOptions options) {
        ConsoleConnection conn = new ConsoleConnection(false, options);
        remoteConsoles.add(conn);
        return conn;
    }

    public static synchronized ConsoleConnection getCommandConsole() {
        if (commandConsole == null)
            commandConsole = new ConsoleConnection(true, new ConnectionOptions());
        return commandConsole;
    }

    public static boolean hasRemoteConsoles() {
        return !remoteConsoles.isEmpty();
    }

    static {
        new Reader().start();
    }

    // -------------------------- instance --------------------------

    private final boolean command;
    private final ConnectionOptions options;

    public ConsoleConnection(boolean command, ConnectionOptions options) {
        super(BUFFER_SIZE, BUFFER_SIZE);
        this.command = command;
        this.options = options;
        if (commandConsole != null)
            log.info("Console is linked to a remote connection. Type '" + CMD_ESCAPE_CHAR + "'<command> to execute command");
    }

    @Override
    protected void closeImpl() {
        remoteConsoles.remove(this);
    }

    @Override
    protected void flushOutput(byte[] buffer, int size) throws IOException {
        System.out.write(buffer, 0, size);
        System.out.flush();
    }

    @Override
    public String toString() {
        return "console";
    }

    private static class Reader extends LoggedThread {
        private static final int PARSE_ANY = 0;
        private static final int PARSE_BEGIN = 1;
        private static final int PARSE_CMD = 2;
        private static final int PARSE_EOLN = 3;

        private int parseState = PARSE_BEGIN;
        private ByteArrayOutputStream cmdBuf;

        private Reader() {
            super("Console");
        }

        @Override
        public void run() {
            byte[] buffer = new byte[BUFFER_SIZE];
            ConsoleConnection[] list = new ConsoleConnection[2];
            while (true) {
                int size;
                try {
                    // reserve space to expand CRLF
                    size = System.in.read(buffer, 0, BUFFER_SIZE / 2);
                } catch (IOException e) {
                    log.log(Level.SEVERE, "Error while reading from console", e);
                    return;
                }
                size = expandCRLF(buffer, size);
                list = remoteConsoles.toArray(list);
                if (list[0] == null) {
                    // no remote console -- work with command console
                    list[0] = commandConsole;
                    list[1] = null;
                } else {
                    // we have remote consoles...
                    if (size > 0 && commandConsole != null) // ... and command console, too
                        size = parse(buffer, size);
                }
                for (ConsoleConnection console : list) {
                    if (console == null)
                        break;
                    if (size < 0)
                        console.in.endOfStream();
                    else
                        console.in.write(buffer, 0, size);
                }
            }
        }

        private int expandCRLF(byte[] buffer, int size) {
            if (size <= 0)
                return size;
            int count = 0;
            for (int i = 0; i < size; i++) {
                if (buffer[i] == '\n')
                    count++;
            }
            int j = size + count;
            for (int i = size - 1; i >= 0; i--) {
                buffer[--j] = buffer[i];
                if (buffer[i] == '\n')
                    buffer[--j] = '\r';
            }
            return size + count;
        }

        private int parse(byte[] buffer, int size) {
            int j = 0;
            for (int i = 0; i < size; i++) {
                byte b = buffer[i];
                boolean isEoln = b == '\r' || b == '\n';
                switch (parseState) {
                    case PARSE_ANY:
                        if (isEoln)
                            parseState = PARSE_BEGIN;
                        break;
                    case PARSE_EOLN:
                        // skip eoln at the end of command
                        if (isEoln)
                            continue;
                        // falls through to process first char on the next line
                    case PARSE_BEGIN:
                        if (b == CMD_ESCAPE_CHAR) {
                            parseState = PARSE_CMD;
                            cmdBuf = new ByteArrayOutputStream(16);
                            continue;
                        }
                        parseState = isEoln ? PARSE_BEGIN : PARSE_ANY;
                        break;
                    case PARSE_CMD:
                        if (isEoln) {
                            sendCommand();
                            parseState = PARSE_EOLN;
                        } else
                            cmdBuf.write(b);
                        continue;
                }
                buffer[j++] = b;
            }
            return j;
        }

        private void sendCommand() {
            cmdBuf.write('\n');
            commandConsole.in.write(cmdBuf.toByteArray());
            cmdBuf = null;
        }
    }
}
