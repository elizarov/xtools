package org.avrbuddy.hex;

import java.io.ByteArrayOutputStream;

/**
 * @author Roman Elizarov
 */
public class HexUtil {
    public static final String HEX_STRING = "0123456789ABCDEF";
    public static final char[] HEX_ARRAY = HEX_STRING.toCharArray();

    public static StringBuilder appendByte(StringBuilder sb, byte b) {
        sb.append(HEX_ARRAY[(b >> 4) & 0x0f]).append(HEX_ARRAY[b & 0x0f]);
        return sb;
    }

    public static String formatByte(byte b) {
        return appendByte(new StringBuilder(2), b).toString();
    }

    public static byte parseByte(char c1, char c2) {
        return (byte)((parseNibble(c1) << 4) | parseNibble(c2));
    }

    public static int parseNibble(char c) {
        int x = HEX_STRING.indexOf(Character.toUpperCase(c));
        if (x < 0)
            throw new IllegalArgumentException("Invalid hex char: " + c);
        return x;
    }

    public static StringBuilder appendNibbles(StringBuilder sb, int x, int count) {
        for (int i = count * 4; (i -= 4) >= 0;)
            sb.append(HEX_ARRAY[(x >> i) & 0x0f]);
        return sb;
    }

    public static String formatNibbles(int x, int count) {
        return appendNibbles(new StringBuilder(count), x, count).toString();
    }

    public static int parseNibbles(String s, int from, int to) {
        int result = 0;
        for (int i = from; i < to; i++) {
            result <<= 4;
            result |= parseNibble(s.charAt(i));
        }
        return result;
    }

    public static StringBuilder appendBytes(StringBuilder sb, byte[] bytes, int from, int to) {
        for (int i = from; i < to; i++)
            appendByte(sb, bytes[i]);
        return sb;
    }

    public static String formatBytes(byte[] bytes) {
        return formatBytes(bytes, 0, bytes.length);
    }

    public static String formatBytes(byte[] bytes, int from, int to) {
        return appendBytes(new StringBuilder((to - from) * 2), bytes, from, to).toString();
    }

    public static byte[] parseBytes(String s) {
        return parseBytes(s, 0, s.length());
    }

    public static byte[] parseBytes(String s, int from, int to) {
        int len = to - from;
        int ofs = from - len % 2;
        int n = (len + 1) / 2;
        byte[] bytes = new byte[n];
        for (int i = 0; i < n; i++) {
            int j = ofs + 2 * i;
            bytes[i] = parseByte(j >= from ? s.charAt(j) : '0', s.charAt(j + 1));
        }
        return bytes;
    }

    public static StringBuilder appendAscii(StringBuilder sb, byte[] data, int from, int to) {
        for (int i = from; i < to; i++) {
            byte b = data[i];
            if (b >= 0x20 && b < 0x7f && b != '\\')
                sb.append((char) b);
            else if (b == 0x0d)
                sb.append('\\').append('r');
            else if (b == 0x0a)
                sb.append('\\').append('n');
            else {
                sb.append('\\');
                appendByte(sb, b);
            }
        }
        return sb;
    }

    public static String formatAscii(byte[] data) {
        return formatAscii(data, 0, data.length);
    }

    public static String formatAscii(byte[] data, int from, int to) {
        return appendAscii(new StringBuilder(), data, from, to).toString();
    }

    public static byte[] parseAscii(String s) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int p = 0;
        while (p < s.length()) {
            char ch = s.charAt(p++);
            if (ch == '\\') {
                if (p >= s.length())
                    throw new IllegalArgumentException("Missing char after backslash");
                ch  = s.charAt(p++);
                if (ch == 'r')
                    out.write(0x0d);
                else if (ch == 'n')
                    out.write(0x0a);
                else {
                    if (p >= s.length())
                        throw new IllegalArgumentException("Missing second char after backslash");
                    out.write((parseNibble(ch) << 4) | parseNibble(s.charAt(p++)));
                }
            } else if (ch >= 0x20 && ch < 0x7f)
                out.write(ch);
            else
                throw new IllegalArgumentException("Invalid data character: " + ch + "(code " + Integer.toHexString(ch) + ")");
        }
        return out.toByteArray();
    }

    public static short getBigEndianShort(byte[] frame, int i) {
        return (short)(((frame[i] & 0xff) << 8) | (frame[i + 1] & 0xff));
    }
}
