package org.mocaris.coins.wallet.util;

/**
 * @author mocaris
 * @date 2020/2/27 22:28
 */
public class Utils {

    public static String byteToHex(byte b) {
        String hex = Integer.toHexString(b & 0xFF);
        if (hex.length() < 2) {
            hex = "0" + hex;
        }
        return hex;
    }
}
