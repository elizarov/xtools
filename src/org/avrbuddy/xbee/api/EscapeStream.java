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

package org.avrbuddy.xbee.api;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Roman Elizarov
 */
class EscapeStream extends OutputStream {
    private final OutputStream out;

    public EscapeStream(OutputStream out) {
        this.out = out;
    }

    @Override
    public void write(int b) throws IOException {
        switch (b) {
        case XBeeUtil.FRAME_START:
        case XBeeUtil.ESCAPE:
        case XBeeUtil.XON:
        case XBeeUtil.XOFF:
            out.write(XBeeUtil.ESCAPE);
            b = b ^ XBeeUtil.XOR;
        }
        out.write(b);
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }
}

