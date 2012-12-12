package org.avrbuddy.xbee.api;

/**
 * @author Roman Elizarov
 */
public interface XBeeFrameListener<F> {
    public void frameReceived(F frame);
    public void connectionClosed();
}
