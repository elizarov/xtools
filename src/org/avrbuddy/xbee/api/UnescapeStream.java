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
import java.io.InputStream;

/**
 * @author Roman Elizarov
 */
class UnescapeStream extends InputStream {
    private final InputStream in;

    UnescapeStream(InputStream in) {
        this.in = in;
    }

    @Override
    public int read() throws IOException {
        int b = in.read();
        if (b == XBeeUtil.ESCAPE) {
            b = in.read();
            if (b >= 0)
                b ^= XBeeUtil.XOR;
        }
        return b;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
