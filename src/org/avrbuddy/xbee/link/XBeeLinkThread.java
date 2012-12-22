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

package org.avrbuddy.xbee.link;

import org.avrbuddy.log.LoggedThread;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;

/**
* @author Roman Elizarov
*/
class XBeeLinkThread extends LoggedThread {
    private static final int BUF_SIZE = 1024;
    private static final long AGGREGATION_DELAY = 2; // wait to produce larger packets

    private final InputStream in;
    private final OutputStream out;
    private XBeeLinkThread other;

    public XBeeLinkThread(String name, InputStream in, OutputStream out) {
        super(name);
        this.in = in;
        this.out = out;
    }

    public void setOther(XBeeLinkThread other) {
        this.other = other;
    }

    private void close() {
        try {
            in.close();
        } catch (IOException e) {
            log.log(Level.WARNING, "Failed to close input", e);
        }
        try {
            out.close();
        } catch (IOException e) {
            log.log(Level.WARNING, "Failed to close output", e);
        }
    }

    @Override
    public void run() {
        byte[] buf = new byte[BUF_SIZE];
        try {
        main_loop:
            while (!Thread.interrupted()) {
                int first = in.read();
                if (first < 0)
                    break;
                buf[0] = (byte) first;
                int i = 1;
                while (i < BUF_SIZE) {
                    int available = in.available();
                    if (available == 0) {
                        try {
                            Thread.sleep(AGGREGATION_DELAY);
                        } catch (InterruptedException e) {
                            break main_loop;
                        }
                        // recheck after delay if still nothing available
                        available = in.available();
                        if (available == 0)
                            break;
                    }
                    int n = Math.min(BUF_SIZE - i, available);
                    n = in.read(buf, i, n);
                    if (n < 0)
                        break main_loop;
                    i += n;
                }
                out.write(buf, 0, i);
                out.flush();
            }
        } catch (EOFException e) {
            // just quit
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed", e);
        }
        close();
        other.close();
    }
}
