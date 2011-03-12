package org.avrbuddy.xbee.api;

import org.avrbuddy.util.HexUtil;

import java.util.Arrays;
import java.util.List;

/**
 * @author Roman Elizarov
 */
public class XBeeUtil {
    static final byte FRAME_START = 0x7e;
    static final byte ESCAPE = 0x7d;
    static final byte XON = 0x11;
    static final byte XOFF = 0x13;
    static final byte XOR = 0x20;

    private static final List<String> ASCII_AT_COMMANDS = Arrays.asList("NI", "ND", "DN");

    public static String formatAtValue(String atCommand, byte[] data, int from, int to) {
        return ASCII_AT_COMMANDS.contains(atCommand) ? HexUtil.formatAscii(data, from, to) : HexUtil.formatBytes(data, from, to);
    }
}
