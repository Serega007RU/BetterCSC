// 
// Decompiled by Procyon v0.5.36
// 

package ru.serega007;

import dev.xdark.clientapi.event.render.ArmorRender;
import dev.xdark.clientapi.event.render.HungerRender;
import dev.xdark.clientapi.event.render.HealthRender;
import dev.xdark.clientapi.event.chat.ChatSend;
import dev.xdark.clientapi.event.render.PlayerListRender;
import dev.xdark.clientapi.opengl.GlStateManager;
import dev.xdark.clientapi.text.Text;
import dev.xdark.clientapi.text.TextFormatting;
import dev.xdark.clientapi.ClientApi;
import dev.xdark.clientapi.event.Listener;
import dev.xdark.clientapi.entry.ModMain;

import java.util.Collections;

public final class CSCHealth implements ModMain, Listener {
    private boolean hp;
    
    public CSCHealth() {
        this.hp = true;
    }

    @Override
    public void load(final ClientApi api) {
        api.chat().printChatMessage(Text.of("[CSCHealth] - загружен, /hp что бы вкл/выкл", TextFormatting.GOLD));
        ChatSend.BUS.register(this, chatSend -> {
            if (chatSend.isCommand()) {
                if (chatSend.getMessage().startsWith("/hp")) {
                    chatSend.setCancelled(true);
                    if (this.hp) {
                        api.chat().printChatMessage(Text.of("[CSCHealth] - отключён", TextFormatting.GOLD));
                    } else {
                        api.chat().printChatMessage(Text.of("[CSCHealth] - включён", TextFormatting.GOLD));
                    }
                    this.hp = !this.hp;
                }
            }
        }, 100);
        HealthRender.BUS.register(this, healthRender -> {
            if (this.hp) {
                healthRender.setCancelled(true);
                String hp = String.format("%.1f", api.minecraft().getPlayer().getHealth()) + "/" + String.format("%.1f", api.minecraft().getPlayer().getMaxHealth());
                int percent = (int)(api.minecraft().getPlayer().getHealth() / api.minecraft().getPlayer().getMaxHealth() * 100.0f);
                int color = getColor(percent);
                api.fontRenderer().drawString(String.join("", Collections.nCopies(25, "█")), api.resolution().getScaledWidth() / 2.0f - 88.0f, api.resolution().getScaledHeight() - 40.0f, 11184810, true);
                api.fontRenderer().drawString(String.join("", Collections.nCopies((int) (percent * 0.25), "█")), api.resolution().getScaledWidth() / 2.0f - 88.0f, api.resolution().getScaledHeight() - 40.0f, color, true);
                api.fontRenderer().drawString( hp, api.resolution().getScaledWidth() / 2.0F - ((float) hp.length() * 3), api.resolution().getScaledHeight() - 50.0f, 0xFFFFFF, true);
            }
        }, 100);
        HungerRender.BUS.register(this, hungerRender -> {
            if (this.hp) hungerRender.setCancelled(true);
        }, 100);
        ArmorRender.BUS.register(this, armorRender -> {
            if (this.hp) armorRender.setCancelled(true);
        }, 100);
        PlayerListRender.BUS.register(this, playerListRender -> {
            if (playerListRender.isCancelled()/* && KeyboardHelper.isShiftKeyDown()*/) {
                playerListRender.setCancelled(false);
            }
        }, -9999);
    }

    @Override
    public void unload() {
        this.hp = false;
        HungerRender.BUS.unregisterAll(this);
        HealthRender.BUS.unregisterAll(this);
        ChatSend.BUS.unregisterAll(this);
        ArmorRender.BUS.unregisterAll(this);
        PlayerListRender.BUS.unregisterAll(this);
    }

    public int getColor(int percent) {
        int red = (int) ((percent > 50 ? 1 - 2 * (percent - 50) / 100.0 : 1.0) * 255);
        int green = (int) ((percent > 50 ? 1.0 : 2 * percent / 100.0) * 255);
//      int blue = 0;
        red = (red << 16) & 0x00FF0000; //Shift red 16-bits and mask out other stuff
        green = (green << 8) & 0x0000FF00; //Shift Green 8-bits and mask out other stuff
        int blue = 0/* & 0x000000FF*/; //Mask out anything not blue.

        return 0xFF000000 | red | green | blue; //0xFF000000 for 100% Alpha. Bitwise OR everything together.
    }
}