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

import org.avrbuddy.log.Log;

import java.io.IOException;

/**
 * @author Roman Elizarov
 */
public abstract class SerialConnection extends Connection {
    public static final int FLOW_CONTROL_IN = 1;
    public static final int FLOW_CONTROL_OUT = 2;

    public void setHardwareFlowControl(int mode) throws IOException {}

    public static SerialConnection open(String port, ConnectionOptions options) throws IOException {
        Log.getLogger(SerialConnection.class).info("Opening serial port " + port + " at " + options.getBaud());
        return new SerialConnectionImpl(port, options.getBaud());
    }
}
