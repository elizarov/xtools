package org.avrbuddy.xbee.api;

import java.util.Arrays;

/**
 * @author Roman Elizarov
 */
class XBeeFrameListenerList {
    private Object[] listeners = new Object[0];

    public synchronized <F extends XBeeFrame> void addListener(Class<F> frameClass, XBeeFrameListener<F> listener) {
        int i = listeners.length;
        listeners = Arrays.copyOf(listeners, i + 2);
        listeners[i] = frameClass;
        listeners[i + 1] = listener;
    }

    public synchronized <F extends XBeeFrame> void removeListener(Class<F> frameClass, XBeeFrameListener<F> listener) {
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
