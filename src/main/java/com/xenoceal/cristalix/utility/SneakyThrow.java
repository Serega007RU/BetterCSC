/*
 * Decompiled with CFR 0.150.
 */
package com.xenoceal.cristalix.utility;

public final class SneakyThrow {
    public static RuntimeException sneaky(Throwable throwable) {
        try {
            throw throwable;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private SneakyThrow() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}

