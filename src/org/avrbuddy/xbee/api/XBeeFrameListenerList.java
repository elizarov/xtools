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

import java.util.Arrays;

/**
 * @author Roman Elizarov
 */
class XBeeFrameListenerList {
    private Object[] listeners = new Object[0];

    public synchronized <F> void addListener(Class<F> frameClass, XBeeFrameListener<F> listener) {
        int i = listeners.length;
        listeners = Arrays.copyOf(listeners, i + 2);
        listeners[i] = frameClass;
        listeners[i + 1] = listener;
    }

    public synchronized <F> void removeListener(Class<F> frameClass, XBeeFrameListener<F> listener) {
        int n = listeners.length;
        for (int i = 0; i < n; i += 2)
            if (listeners[i] == frameClass && listeners[i + 1] == listener) {
                Object[] listeners = new Object[n - 2];
                System.arraycopy(this.listeners, 0, listeners, 0, i);
                System.arraycopy(this.listeners, i + 2, listeners, i, n - 2 - i);
                this.listeners = listeners;
                return;
            }
    }

    public synchronized Object[] getListeners() {
        return listeners;
    }
}
