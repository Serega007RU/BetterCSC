/*
 * Decompiled with CFR 0.150.
 */
package com.xenoceal.cristalix.utility;

import java.lang.reflect.Field;
import sun.misc.Unsafe;

public final class UnsafeUtil {
    private static Unsafe unsafe;

    public static Unsafe get() {
        return unsafe;
    }

    private UnsafeUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Unsafe unsafe = (Unsafe)field.get(null);
            if (unsafe == null) {
                throw new IllegalStateException("Unable to locate unsafe instance");
            }
            UnsafeUtil.unsafe = unsafe;
        }
        catch (Throwable throwable) {
            throw SneakyThrow.sneaky(throwable);
        }
    }
}

