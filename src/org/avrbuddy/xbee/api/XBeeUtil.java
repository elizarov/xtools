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

import org.avrbuddy.hex.HexUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * @author Roman Elizarov
 */
public class XBeeUtil {
    public static final int STATUS_TIMEOUT = 0x100;

    static final byte FRAME_START = 0x7e;
    static final byte ESCAPE = 0x7d;
    static final byte XON = 0x11;
    static final byte XOFF = 0x13;
    static final byte XOR = 0x20;

    private static final List<String> ASCII_AT_COMMANDS = Arrays.asList("NI", "ND", "DN");

    public static String formatAtValue(String atCommand, byte[] data) {
        return formatAtValue(atCommand, data, 0, data.length);
    }

    public static String formatAtValue(String atCommand, byte[] data, int from, int to) {
        return ASCII_AT_COMMANDS.contains(atCommand.toUpperCase(Locale.US)) ?
                HexUtil.formatAscii(data, from, to) :
                HexUtil.formatBytes(data, from, to);
    }

    public static byte[] parseAtValue(String atCommand, String value) {
        return ASCII_AT_COMMANDS.contains(atCommand.toUpperCase(Locale.US)) ?
                HexUtil.parseAscii(value) :
                HexUtil.parseBytes(value);
    }

    public static String formatStatus(int status) {
        switch (status) {
            case STATUS_TIMEOUT:
                return "TIMEOUT";
            case XBeeAtResponseFrame.STATUS_OK:
                return "OK";
            case XBeeAtResponseFrame.STATUS_ERROR:
                return "ERROR";
            case XBeeAtResponseFrame.STATUS_INVALID_COMMAND:
                return "INVALID COMMAND";
            case XBeeAtResponseFrame.STATUS_TX_FAILURE:
                return "TX FAILURE";
            case XBeeAtResponseFrame.STATUS_INVALID_PARAMETER:
                return "INVALID PARAMETER";
            default:
                return HexUtil.formatByte((byte) status);
        }
    }

    public static int getStatus(XBeeFrameWithId[] responses) {
        int status = XBeeAtResponseFrame.STATUS_OK;
        for (XBeeFrameWithId response : responses) {
            if (response == null)
                return STATUS_TIMEOUT;
            status = Math.max(status, response.getStatus() & 0xff);
        }
        return status;
    }

    public static void checkStatus(XBeeFrameWithId[] responses) throws XBeeException {
        int status = getStatus(responses);
        if (status != XBeeAtResponseFrame.STATUS_OK)
            throw new XBeeException(formatStatus(status));
    }
}
