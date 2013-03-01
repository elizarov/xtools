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
import org.avrbuddy.util.WrongFormatException;

import java.util.Arrays;

/**
 * @author Roman Elizarov
 */
public class XBeeAddress implements Comparable<XBeeAddress> {
    public static final int ADDRESS_LENGTH = 10;

    public static final String BROADCAST_STRING = "*";
    public static final String COORDINATOR_STRING = "0";

    public static final XBeeAddress BROADCAST = new XBeeAddress(new byte[] {0, 0, 0, 0, 0, 0, (byte)0xff, (byte)0xff}, 0, BROADCAST_STRING);
    public static final XBeeAddress COORDINATOR = new XBeeAddress(new byte[] {0, 0, 0, 0, 0, 0, 0, 0}, 0, COORDINATOR_STRING);

    public static final String S_PREFIX = "[";
    public static final String S_SUFFIX = "]";

    private final byte[] address = new byte[ADDRESS_LENGTH];
    private String string;

    public static XBeeAddress valueOf(byte[] data, int offset) {
        return isBroadcast(data, offset) ? BROADCAST : new XBeeAddress(data, offset, null);
    }

    public static XBeeAddress valueOf(byte[] highAddressBytes, byte[] lowAddressBytes) {
        byte[] data = new byte[8];
        System.arraycopy(highAddressBytes, 0, data, 0, 4);
        System.arraycopy(lowAddressBytes, 0, data, 4, 4);
        return valueOf(data, 0);
    }

    private static boolean isBroadcast(byte[] data, int offset) {
        if (data.length - offset < 8)
            return false;
        for (int i = 0; i < 8; i++)
            if (data[i + offset] != BROADCAST.address[i])
                return false;
        return true;
    }

    public static XBeeAddress valueOf(String s) {
        if (s.equals(BROADCAST_STRING))
             return BROADCAST;
        if (s.equals(COORDINATOR_STRING))
             return COORDINATOR;
        if (!s.startsWith(S_PREFIX) || !s.endsWith(S_SUFFIX))
            throw new WrongFormatException("Address must be enclosed in " + S_PREFIX + "..." + S_SUFFIX);
        s = s.substring(S_PREFIX.length(), s.length() - S_SUFFIX.length());
        if (s.equals(BROADCAST_STRING))
            return BROADCAST;
        if (s.equals(COORDINATOR_STRING))
            return COORDINATOR;
        XBeeAddress result = new XBeeAddress(s);
        if (result.equals(BROADCAST))
            return BROADCAST;
        if (result.equals(COORDINATOR))
            return COORDINATOR;
        return result;
    }

    private XBeeAddress() {
        address[8] = (byte)0xff;
        address[9] = (byte)0xfe;
    }

    private XBeeAddress(byte[] data, int offset, String s) {
        this();
        System.arraycopy(data, offset, address, 0, Math.min(data.length - offset, address.length));
        this.string = s;
    }

    private XBeeAddress(String s) {
        this();
        int i= 0;
        int p = 0;
        while (i < address.length && p < s.length() - 1) {
            // optional separator
            if (s.charAt(p) == ':')
                p++;
            address[i++] = HexUtil.parseByte(s.charAt(p++), s.charAt(p++));
        }
        if (p < s.length())
            throw new WrongFormatException("Extra characters in address string [" + s + "]");
        if (i < 8)
            throw new WrongFormatException("Address string is too short. At least 8 hex bytes are expected [" + s + "]");
    }

    public byte[] getAddressBytes() {
        return address.clone();
    }

    public byte[] getHighAddressBytes() {
        return Arrays.copyOfRange(address, 0, 4);
    }

    public byte[] getLowAddressBytes() {
        return Arrays.copyOfRange(address, 4, 8);
    }

    @Override
    public String toString() {
        if (string != null)
            return string;
        StringBuilder sb = new StringBuilder();
        sb.append(S_PREFIX);
        for (int i = 0; i < address.length; i++) {
            if (i == 8)
                sb.append(':');
            HexUtil.appendByte(sb, address[i]);
        }
        sb.append(S_SUFFIX);
        string = sb.toString();
        return string;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        byte[] a2 = ((XBeeAddress) o).address;
        for (int i = 0; i < 8; i++)
            if (address[i] != a2[i])
                return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        for (int i = 0; i < 8; i++)
            result = 31 * result + address[i];
        return result;
    }

    @Override
    public int compareTo(XBeeAddress o) {
        for (int i = 0; i < 8; i++) {
            int j = (address[i] & 0xff) - (o.address[i] & 0xff);
            if (j != 0)
                return j;
        }
        return 0;
    }
}
