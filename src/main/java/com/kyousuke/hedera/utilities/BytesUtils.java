package com.kyousuke.hedera.utilities;

public class BytesUtils {
    public static byte[] copyBytes(int start, int length, byte[] bytes) {
        byte[] rv = new byte[length];

        for (int i = 0; i < length; i++) {
            rv[i] = bytes[start + i];
        }

        return rv;
    }
}
