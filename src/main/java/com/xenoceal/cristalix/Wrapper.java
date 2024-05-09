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

import io.netty.buffer.ByteBuf;

public final class Wrapper {
    static Class<?> unpooledSlicedByteBufClass = Reflection.getClass("UnpooledSlicedByteBuf");
    public static ByteBuf unwrapBuffer(ByteBuf buf) {
        Class<?> bufferClass = buf.getClass();
        if (bufferClass.isAssignableFrom(unpooledSlicedByteBufClass)) {
            return (ByteBuf) Reflection.invoke("unwrapByteBuff", buf);
        } else {
            return buf;
        }
    }

    private Wrapper() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}

