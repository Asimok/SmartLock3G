package com.example.SmartLock3G.tools;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Author: areful
 * Date: 2019/3/26
 */
public class HexStringUtil {
    private static final char[] HEX_CHAR_TABLE = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };
    private static final Map<Character, Byte> MAP = new HashMap<>();

    static {
        for (int i = 0; i < HEX_CHAR_TABLE.length; i++) {
            char c = HEX_CHAR_TABLE[i];
            MAP.put(c, (byte) i);
        }
    }

    public static String toHexString(byte[] array) {
        StringBuilder sb = new StringBuilder();
        for (byte b : array) {
            sb.append(HEX_CHAR_TABLE[(b & 0xf0) >> 4]);
            sb.append(HEX_CHAR_TABLE[b & 0x0f]);
        }
        return sb.toString();
    }

    public static byte[] toByteArray(String hexString) {
        byte[] result = new byte[hexString.length() / 2];
        for (int i = 0; i < hexString.length() / 2; i++) {
            char hi = hexString.charAt(i * 2);
            char lo = hexString.charAt(i * 2 + 1);
            result[i] = (byte) ((MAP.get(hi) << 4) + MAP.get(lo));
        }
        return result;
    }

    public static void main(String[] args) {
        String hexString = toHexString(new byte[]{-128, -1, 1, 127,});
        System.out.println(hexString);

        byte[] arr = toByteArray(hexString);
        System.out.println(Arrays.toString(arr));
    }
}