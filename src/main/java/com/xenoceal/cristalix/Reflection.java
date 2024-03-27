package com.xenoceal.cristalix;

import com.xenoceal.cristalix.utility.SneakyThrow;
import com.xenoceal.cristalix.utility.UnsafeUtil;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import dev.xdark.clientapi.util.EnumHand;
import sun.misc.Unsafe;

public final class Reflection {
    private static final MethodHandles.Lookup LOOKUP;
    private static final MethodHandle MH_CLASS_FN;
    private static final Map<String, Class<?>> classes = new HashMap<>();
    private static final Map<String, MethodHandle> handles = new HashMap<>();

    public static void initialize() {
        try {
            addClass("Minecraft", "THMPruG");
            addClass("ClientConnection", "QZasfYI");
            addClass("Packet", "eGDSUFY");
            addClass("EnumHand", "zpKTKhH");
            addClass("CPacketPlayerTryUseItem", "WlbASZU");
            addClass("InventoryPlayer", "lKJPfSy");
            addHandle("getMinecraft", LOOKUP.findStatic(getClass("Minecraft"), "qzAMUds", MethodType.methodType(getClass("Minecraft"))));
            addHandle("getClientConnection", LOOKUP.findVirtual(getClass("Minecraft"), "qzAMUds", MethodType.methodType(getClass("ClientConnection"))));
            addHandle("sendPacket", LOOKUP.findVirtual(getClass("ClientConnection"), "qzAMUds", MethodType.methodType(Void.TYPE, getClass("Packet"))));
            addHandle("CPacketPlayerTryUseItem", LOOKUP.findConstructor(getClass("CPacketPlayerTryUseItem"), MethodType.methodType(Void.TYPE, getClass("EnumHand"))));
            addHandle("changeActiveSlot", LOOKUP.findSetter(getClass("InventoryPlayer"), "ycqpnTx", Integer.TYPE));
        } catch (Throwable var1) {
            throw SneakyThrow.sneaky(var1);
        }
    }

    public static Object invoke(String var0, Object... var1) {
        try {
            MethodHandle var2 = handles.get(var0);
            return var2.invokeWithArguments(var1);
        } catch (Throwable var3) {
            throw SneakyThrow.sneaky(var3);
        }
    }

    private static Class<?> findClass(String var0) {
        try {
            return (Class<?>) MH_CLASS_FN.invoke(var0, true, ClassLoader.getSystemClassLoader());
        } catch (Throwable var2) {
            throw SneakyThrow.sneaky(var2);
        }
    }

    public static Class<?> getClass(String var0) {
        return classes.get(var0);
    }

    private static void addClass(String var0, String var1) {
        try {
            classes.put(var0, findClass(var1));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static void addHandle(String var0, MethodHandle var1) {
        handles.put(var0, var1);
    }

    private static Field getField(Class<?> var0, String var1, Class<?> var2) {
        Field var3 = null;
        Field[] var4 = var0.getDeclaredFields();

        for (Field var7 : var4) {
            if (var7.getName().equals(var1) && var7.getType() == var2) {
                var3 = var7;
                break;
            }
        }

        if (var3 != null) {
            var3.setAccessible(true);
        }

        return var3;
    }

    private Reflection() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    static {
        try {
            Unsafe var0 = UnsafeUtil.get();
            Field var1 = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            var1.setAccessible(true);
            LOOKUP = (MethodHandles.Lookup)var0.getObject(var0.staticFieldBase(var1), var0.staticFieldOffset(var1));
            MH_CLASS_FN = LOOKUP.findStatic(Class.class, "forName", MethodType.methodType(Class.class, String.class, Boolean.TYPE, ClassLoader.class));
        } catch (Exception var2) {
            try {
                throw SneakyThrow.sneaky(var2);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }
}
