package org.avrbuddy.avr;

import java.io.IOException;

/**
 * @author Roman Elizarov
 */
public class AvrSyncException extends IOException {
    public AvrSyncException(String message) {
        super(message);
    }
}
