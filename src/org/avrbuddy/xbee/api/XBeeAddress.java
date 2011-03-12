package org.avrbuddy.xbee.api;

import org.avrbuddy.util.HexUtil;

import java.util.Arrays;

/**
 * @author Roman Elizarov
 */
public class XBeeAddress {
    public static final int ADDRESS_LENGTH = 10;

    public static final XBeeAddress BROADCAST = new XBeeAddress(new byte[] {0, 0, 0, 0, 0, 0, (byte)0xff, (byte)0xff}, 0);
    public static final XBeeAddress COORDINATOR = new XBeeAddress(new byte[] {0, 0, 0, 0, 0, 0, 0, 0}, 0);

    public static final String BROADCAST_STRING = "*";

    private final byte[] address = new byte[ADDRESS_LENGTH];

    public static XBeeAddress valueOf(byte[] data, int offset) {
        if (data.length - offset >= ADDRESS_LENGTH)
            for (int i = 0; i < ADDRESS_LENGTH; i++)
                if (data[i + offset] != BROADCAST.address[i])
                    return new XBeeAddress(data, offset);
        return BROADCAST;
    }

    public static XBeeAddress valueOf(String s) {
        if (s.equals(BROADCAST_STRING))
             return BROADCAST;
        XBeeAddress result = new XBeeAddress(s);
        if (result.equals(BROADCAST))
            return BROADCAST;
        return result;
    }

    private XBeeAddress() {
        address[8] = (byte)0xff;
        address[9] = (byte)0xfe;
    }

    private XBeeAddress(byte[] data, int offset) {
        this();
        System.arraycopy(data, offset, address, 0, Math.min(data.length - offset, address.length));
    }

    private XBeeAddress(String s) {
        this();
        if (s.length() < 16)
            throw new IllegalArgumentException("Address string is two short. Expected <64bits>[:<16bits>]");
        int p = 0;
        for (int i = 0; i < address.length && p < s.length(); i++) {
            if (i == 8)
                if (s.charAt(p++) != ':')
                    throw new IllegalArgumentException("Missing separator. Expected <64bits>:<16bits>");
            address[i] = HexUtil.parseByte(s.charAt(p++), s.charAt(p++));
        }
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
        if (this == BROADCAST)
            return BROADCAST_STRING;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < address.length; i++) {
            if (i == 8)
                sb.append(':');
            HexUtil.appendByte(sb, address[i]);
        }
        return sb.toString();
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
}
