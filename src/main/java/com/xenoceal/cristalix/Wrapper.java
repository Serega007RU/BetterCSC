/*
 * Decompiled with CFR 0.150.
 *
 * Could not load the following classes:
 *  dev.xdark.clientapi.entity.Entity
 *  dev.xdark.clientapi.entity.EntityPlayer
 *  dev.xdark.clientapi.entity.EntityPlayerSP
 *  dev.xdark.clientapi.game.Minecraft
 *  dev.xdark.clientapi.inventory.Container
 *  dev.xdark.clientapi.inventory.Inventory
 *  dev.xdark.clientapi.math.AxisAlignedBB
 *  dev.xdark.clientapi.math.BlockPos
 *  dev.xdark.clientapi.render.Tessellator
 *  dev.xdark.clientapi.texture.DynamicTexture
 *  dev.xdark.clientapi.util.EnumFacing
 *  dev.xdark.clientapi.util.EnumHand
 */
package com.xenoceal.cristalix;

public final class Wrapper {
    public static Object getMinecraft() {
        return Reflection.invoke("getMinecraft");
    }

    public static Object getClientConnection() {
        return Reflection.invoke("getClientConnection", Wrapper.getMinecraft());
    }

    public static void sendPacket(Object object) {
        Reflection.invoke("sendPacket", Wrapper.getClientConnection(), object);
    }

    public static void changeActiveSlot(Object inventory, Object slot) {
        Reflection.invoke("changeActiveSlot", inventory, slot);
    }

    public static Object CPacketPlayerTryUseItem(Object enumHand) {
        return Reflection.invoke("CPacketPlayerTryUseItem", enumHand);
    }

    private Wrapper() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}

