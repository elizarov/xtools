package org.avrbuddy.xbee.cmd;

/**
 * @author Roman Elizarov
 */
public class InvalidCommandException extends IllegalArgumentException {
    public InvalidCommandException(String s) {
        super(s);
    }
}
