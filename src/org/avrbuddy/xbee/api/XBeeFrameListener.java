package org.avrbuddy.xbee.api;

/**
 * @author Roman Elizarov
 */
public interface XBeeFrameListener<F extends XBeeFrame> {
    public void frameReceived(F frame);
}
