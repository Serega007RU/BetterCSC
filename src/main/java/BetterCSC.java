import dev.xdark.clientapi.entity.EntityPlayerSP;
import dev.xdark.clientapi.event.chat.ChatReceive;
import dev.xdark.clientapi.event.gui.ScreenDisplay;
import dev.xdark.clientapi.event.inventory.WindowClick;
import dev.xdark.clientapi.event.network.PluginMessage;
import dev.xdark.clientapi.event.render.*;
import dev.xdark.clientapi.event.chat.ChatSend;
import dev.xdark.clientapi.input.KeyboardHelper;
import dev.xdark.clientapi.item.Item;
import dev.xdark.clientapi.item.ItemStack;
import dev.xdark.clientapi.text.Text;
import dev.xdark.clientapi.text.TextFormatting;
import dev.xdark.clientapi.ClientApi;
import dev.xdark.clientapi.event.Listener;
import dev.xdark.clientapi.entry.ModMain;
import dev.xdark.feder.NetUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
public class BetterCSC implements ModMain, Listener {
    private boolean hp = true;

    private Text lastMessage = null;
//    private ScheduledExecutorService task = null;

    private boolean enableUP = false;
    private ScheduledExecutorService taskUP = null;
    private int periodUP = 100000;
    private int countUp = 0;

    private boolean enableBuy = false;
    private ScheduledExecutorService taskBuy = null;
    private int periodBuy = 16666;

    private long allBets = 0;
    private boolean countBets = false;

    private boolean forceSingleWindow = true;

    @Override
    public void load(ClientApi api) {
        api.chat().printChatMessage(Text.of("[BetterCSC] - загружен", TextFormatting.GOLD));
        if (api.minecraft().currentScreen() instanceof asO) api.minecraft().displayScreen(new us());
        ChatSend.BUS.register(this, chatSend -> {
            if (chatSend.isCommand()) {
                String msg = chatSend.getMessage();

                if (msg.startsWith("/hp")) {
                    chatSend.setCancelled(true);
                    if (this.hp)
                        api.chat().printChatMessage(Text.of("[BetterCSC] - ", TextFormatting.GOLD, "отключён", TextFormatting.RED));
                    else
                        api.chat().printChatMessage(Text.of("[BetterCSC] - ", TextFormatting.GOLD, "включён", TextFormatting.GREEN));
                    this.hp = !this.hp;
                } else if (msg.startsWith("/upgrade")) {
                    chatSend.setCancelled(true);
                    api.clientConnection().sendPayload("csc:upgrade", Unpooled.buffer());
                } else if (msg.startsWith("/up")) {
                    chatSend.setCancelled(true);
                    int count;
                    try {
                        count = Integer.parseInt(msg.replace("/up ", ""));
                    } catch (Exception e) {
                        api.chat().printChatMessage(Text.of("[BetterCSC] Укажите число", TextFormatting.RED));
                        return;
                    }
                    EntityPlayerSP player = api.minecraft().getPlayer();
                    if (!enableUP) {
                        try {
                            player.getInventory().getCurrentItem().getItem().getId();
                        } catch (Exception e) {
                            api.chat().printChatMessage(Text.of("[BetterCSC] - В руках отсутствует предмет", TextFormatting.RED));
                            return;
                        }
                        api.chat().printChatMessage(Text.of("[BetterCSC] - Быстрый апгрейд ", TextFormatting.GOLD, "включён", TextFormatting.GREEN));
                        countUp = 0;
                        enableUP = true;
                        int slot = player.getInventory().getActiveSlot();
                        int id = player.getInventory().getCurrentItem().getItem().getId();

                        taskUP = api.threadManagement().newSingleThreadedScheduledExecutor();
                        taskUP.scheduleAtFixedRate(() -> {
                            if (!enableUP) return;
                            countUp++;
                            if (countUp > count) {
                                enableUP = false;
                                api.chat().printChatMessage(Text.of("[BetterCSC] - Быстрый апгрейд ", TextFormatting.GOLD, "выключен", TextFormatting.RED, ", ", TextFormatting.GOLD, "достигли заданного числа", TextFormatting.GREEN));
                                if (taskUP != null) taskUP.shutdown();
                                taskUP = null;
                                countUp = 0;
                                return;
                            }
                            ByteBuf buffer;
                            ByteBuf $this$writeVarInt$iv = buffer = Unpooled.buffer();
                            NetUtil.writeVarInt(slot, $this$writeVarInt$iv);
                            NetUtil.writeVarInt(id, buffer);
                            api.clientConnection().sendPayload("csc:upgrade", buffer);
                        }, 0, periodUP, TimeUnit.MICROSECONDS);
                    } else {
                        enableUP = false;
                        api.chat().printChatMessage(Text.of("[BetterCSC] - Быстрый апгрейд ", TextFormatting.GOLD, "выключен", TextFormatting.RED));
                        if (taskUP != null) taskUP.shutdown();
                        taskUP = null;
                        countUp = 0;
                    }
                } else if (msg.startsWith("/period up")) {
					chatSend.setCancelled(true);
                    try {
                        periodUP = 1000000 / Integer.parseInt(msg.replace("/period up ", ""));
                    } catch (Exception e) {
                        api.chat().printChatMessage(Text.of("[BetterCSC] Укажите число", TextFormatting.RED));
                        return;
                    }
                    api.chat().printChatMessage(Text.of("[BetterCSC] - Период прокачки настроен на ", TextFormatting.GOLD, String.valueOf(periodUP), TextFormatting.WHITE));
                } else if (msg.startsWith("/period buy")) {
                    chatSend.setCancelled(true);
                    try {
                        periodBuy = 1000000 / Integer.parseInt(msg.replace("/period buy ", ""));
                    } catch (Exception e) {
                        api.chat().printChatMessage(Text.of("[BetterCSC] Укажите число", TextFormatting.RED));
                        return;
                    }
                    api.chat().printChatMessage(Text.of("[BetterCSC] - Период покупки настроен на ", TextFormatting.GOLD, String.valueOf(periodBuy), TextFormatting.WHITE));
                } else if (msg.startsWith("/forcesingleup")) {
                    chatSend.setCancelled(true);
                    forceSingleWindow = !forceSingleWindow;
                    if (forceSingleWindow) api.chat().printChatMessage(Text.of("[BetterCSC] - ForceSingleUp ", TextFormatting.GOLD, "включён", TextFormatting.WHITE));
                    else api.chat().printChatMessage(Text.of("[BetterCSC] - ForceSingleUp ", TextFormatting.GOLD, "выключен", TextFormatting.RED));

                } else if (msg.startsWith("/sendpayload ")) {
                    chatSend.setCancelled(true);
                    api.clientConnection().sendPayload(msg.replace("/sendpayload ", ""), Unpooled.buffer());
                } else if (msg.startsWith("/mod ")) {
                    chatSend.setCancelled(true);
                    try {
                        asE ase;
                        sE se;
                        ClientApi clientApi;
                        List<String> mods = new ArrayList<>();
                        //Получаем доступ к приватному классу ClientApi
//                    try {
//                        Field field = null;
//                        for (Field f : api.getClass().getDeclaredFields()) {
//                            if (f.getName().equals("a") && f.getType().getName().equals("dev.xdark.clientapi.ClientApi")) {
//                                field = f;
//                                break;
//                            }
//                        }
//                        field.setAccessible(true);
//                        clientApi = (ClientApi) field.get(api);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        api.chat().printChatMessage(Text.of(e.getLocalizedMessage(), TextFormatting.DARK_RED));
//                    }
                        //Для начала достаём класс Minecraft (Minecraft.getMinecraft())
                        //Позже получаем доступ к классу asE в котором хранится список модов
                        {
                            Field field = sE.class.getDeclaredField("a");
                            field.setAccessible(true);
                            se = (sE) field.get(null);
                            Method method = null;
                            for (Method m : se.getClass().getDeclaredMethods()) {
                                if (m.getName().equals("a") && m.getReturnType().getName().equals("asE")) {
                                    method = m;
                                    break;
                                }
                            }
                            method.setAccessible(true);
                            ase = (asE) method.invoke(se);
                        }
                        //Получаем доступ к приватному классу ClientApi наследующий класс c
                        {
                            Field field = null;
                            for (Field f : asE.class.getFields()) {
                                if (f.getName().equals("a") && f.getType().getName().equals("dev.xdark.clientapi.ClientApi")) {
                                    field = f;
                                    break;
                                }
                            }
                            clientApi = (ClientApi) field.get(ase);
                        }

                        Map<String, ModMain> modList;
                        //Получаем доступ к списку модов и делаем чо хотим с модом
                        {
                            Field field = null;
                            for (Field f : asE.class.getDeclaredFields()) {
                                if (f.getName().equals("a") && f.getType().getName().equals("java.util.Map")) {
                                    field = f;
                                    break;
                                }
                            }
                            field.setAccessible(true);
                            //noinspection unchecked
                            modList = (Map<String, ModMain>) field.get(ase);
                        }
                        //Получаем доступ ко второму списку модов и делаем чо хотим с модом
                        {
                            Field field = null;
                            for (Field f : asE.class.getDeclaredFields()) {
                                if (f.getName().equals("b") && f.getType().getName().equals("java.util.Map")) {
                                    field = f;
                                    break;
                                }
                            }
                            field.setAccessible(true);
                            //noinspection unchecked
                            modList.putAll((Map<String, ModMain>) field.get(ase));
                        }
                        for (Map.Entry<String, ModMain> mod : modList.entrySet()) {
                            if (msg.startsWith("/mod list")) {
                                mods.add(mod.getKey());
                            } else {
                                String search = msg.toLowerCase();
                                search = search.replace("/mod unload ", "");
                                search = search.replace("/mod load ", "");
                                if (!mod.getKey().toLowerCase().contains(search)) {
                                    continue;
                                }
                                if (msg.startsWith("/mod unload ")) {
                                    api.chat().printChatMessage(Text.of("[BetterCSC] - ", TextFormatting.GOLD, "Выгружаем ", TextFormatting.RED, mod.getKey(), TextFormatting.WHITE));
                                } else {
                                    api.chat().printChatMessage(Text.of("[BetterCSC] - ", TextFormatting.GOLD, "Загружаем ", TextFormatting.GREEN, mod.getKey(), TextFormatting.WHITE));
                                }
                                hw hw = (hw) mod.getValue();
                                ModMain modMain = null;
                                try {
                                    Field field = null;
                                    for (Field f : hw.class.getDeclaredFields()) {
                                        if (f.getName().equals("a") && f.getType().getName().equals("dev.xdark.clientapi.entry.ModMain")) {
                                            field = f;
                                            break;
                                        }
                                    }
                                    field.setAccessible(true);
                                    modMain = (ModMain) field.get(hw);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    api.chat().printChatMessage(Text.of(e.getLocalizedMessage(), TextFormatting.DARK_RED));
                                }
                                if (msg.startsWith("/mod unload ")) {
                                    modMain.unload();
                                } else {
                                    modMain.load(clientApi);
                                }
                            }
                        }
                        if (msg.startsWith("/mod list")) {
                            for (String mod : mods) {
                                api.chat().printChatMessage(Text.of(mod));
                            }
                        }
                        //Какой-то третий список модов (хз зачем он нужен)
//                    try {
//                        Field field = null;
//                        for (Field f : asE.class.getFields()) {
//                            if (f.getName().equals("a") && f.getType().getName().equals("java.util.Collection")) {
//                                field = f;
//                                break;
//                            }
//                        }
//                        //noinspection unchecked
//                        Collection<ModMain> modList = (Collection<ModMain>) field.get(ase);
//                        for (ModMain mod : modList) {
//                            System.out.println("Mod: " + mod);
//                        }
//                        System.out.println("Конец Collection a");
//                    } catch (Exception e) {
//                        throw e;
//                    }
                    } catch (Exception e) {
                        e.printStackTrace();
                        api.chat().printChatMessage(Text.of(e.getLocalizedMessage(), TextFormatting.DARK_RED));
                    }
                }
            }
        }, 100);
        ChatReceive.BUS.register(this, chatReceive -> {
            if (this.hp) {
                String msg = chatReceive.getText().getUnformattedText();
                if (enableUP) {
                    if (msg.contains("Баланс: ") || msg.contains("Вы успешно улучшили предмет")) {
                        chatReceive.setCancelled(true);
                        return;
                    } else if (msg.contains("У вас недостаточно золота на балансе") || msg.contains("Этот предмет нельзя улучшить") || msg.contains("Вы не находитесь в игре") || msg.contains("вы не можете сейчас открыть меню апгрейда")) {
                        chatReceive.setCancelled(true);
                        enableUP = false;
                        if (taskUP != null) taskUP.shutdown();
                        api.chat().printChatMessage(Text.of("[BetterCSC] - Быстрый апгрейд ", TextFormatting.GOLD, "выключен", TextFormatting.RED, ", " + TextFormatting.GOLD, msg, TextFormatting.RED));
                        return;
                    }
                } else if (enableBuy) {
                    if (msg.contains("Баланс: ") || msg.contains("Вы успешно купили предмет")) {
                        chatReceive.setCancelled(true);
                        return;
                    } else if (msg.contains("У вас недостаточно золота на балансе") || msg.contains("Вы уже купили этот предмет")) {
                        chatReceive.setCancelled(true);
                        enableBuy = false;
                        if (taskBuy != null) taskBuy.shutdown();
                        api.chat().printChatMessage(Text.of("[BetterCSC] - Быстрая покупка ", TextFormatting.GOLD, "выключена", TextFormatting.RED, ", ", TextFormatting.GOLD, msg, TextFormatting.RED));
                        return;
                    }
                }

                if (msg.contains("Баланс: ")) {
                    chatReceive.setCancelled(true);
                    lastMessage = chatReceive.getText();
//                    task = api.threadManagement().newSingleThreadedScheduledExecutor();
//                    task.schedule(() -> {
//                        if (lastMessage != null && task != null && !task.isShutdown()) {
//                            api.chat().printChatMessage(chatReceive.getText());
//                            task = null;
//                            lastMessage = null;
//                        }
//                    }, 500, TimeUnit.MILLISECONDS);
                } else if (msg.contains("Вы успешно улучшили предмет") || msg.contains("Вы успешно купили предмет")) {
                    chatReceive.setCancelled(true);
                    lastMessage = null;
//                    if (task != null && !task.isShutdown()) {
//                        task.shutdown();
//                        task = null;
//                    }
                } else if (msg.contains("Ставки выиграли:")) {
                    countBets = true;
                } else if (countBets) {
                    if (msg.contains(" - ")) {
                        String text = msg;
                        text = msg.substring(text.indexOf(" - ") + 3, text.length());
                        allBets += Long.parseLong(text);
                    } else {
                        api.chat().printChatMessage(Text.of("Общая сумма ставок: ", TextFormatting.GOLD, new DecimalFormat("#,###").format(allBets), TextFormatting.GOLD));
                        countBets = false;
                        allBets = 0;
                    }
                }

                if (lastMessage != null) {
                    api.chat().printChatMessage(lastMessage);
                    lastMessage = null;
                }
            }
        }, 100);
        HealthRender.BUS.register(this, healthRender -> {
            if (this.hp && !healthRender.isCancelled()) {
                healthRender.setCancelled(true);
                EntityPlayerSP player = api.minecraft().getPlayer();
                float health = player.getHealth();
                float maxHealth = player.getMaxHealth();
                int percent = (int)(health / maxHealth * 100.0f);
                int color = getColor(percent);
                float w = api.resolution().getScaledWidth(), h = api.resolution().getScaledHeight();

                api.fontRenderer().drawString(String.join("", Collections.nCopies(25, "█")), w / 2.0f - 88.0f, h - 40.0f, 11184810, true);
                api.fontRenderer().drawString(String.join("", Collections.nCopies((int) (percent * 0.25), "█")), w / 2.0f - 88.0f, h - 40.0f, color, true);

//                api.fontRenderer().drawString(api.minecraft().getSession().getName(),
//                        5f, 15.0f,
//                        11184810, true); // Nickname

                float alignment = 0.0F;
                if (percent < 10) alignment = 8.0F;
                else if (percent < 100) alignment = 4.0F;
                api.fontRenderer().drawString(String.format("%d%%", percent),
                        w / 2.0F - 12.0f + alignment, h - 50.0f,
                        color, true); // Percents

                api.fontRenderer().drawString(String.format("%s \u2665", (int)health),
                        w / 2.0F - 88.0F, h - 50.0f,
                        color, true); // Health

                String mh = String.format("%6s \u2665", (int)maxHealth);
                api.fontRenderer().drawString(mh,
                        w / 2.0F + 87.0F - api.fontRenderer().getStringWidth(mh), h - 50.0f,
                        16777215, true); // Max Health

//              api.fontRenderer().drawString( hp, api.resolution().getScaledWidth() / 2.0F - ((float) hp.length() * 3), api.resolution().getScaledHeight() - 50.0f, 0xFFFFFF, true);
            }
        }, -1);
        HungerRender.BUS.register(this, hungerRender -> {
            if (this.hp) hungerRender.setCancelled(true);
        }, -1);
        ArmorRender.BUS.register(this, armorRender -> {
            if (this.hp) armorRender.setCancelled(true);
        }, -1);
        PlayerListRender.BUS.register(this, playerListRender -> {
            if (this.hp && playerListRender.isCancelled()) playerListRender.setCancelled(false);
        }, -1);
        WindowClick.BUS.register(this, windowClick -> {
            if (this.hp) {
                if (KeyboardHelper.isAltKeyDown() && windowClick.getMouseButton() == 2) {
                    if (!enableBuy) {
                        api.chat().printChatMessage(Text.of("[BetterCSC] - Быстрая покупка ", TextFormatting.GOLD, "включена", TextFormatting.GREEN));
                        enableBuy = true;

                        taskBuy = api.threadManagement().newSingleThreadedScheduledExecutor();
                        AtomicInteger windowId = new AtomicInteger(windowClick.getWindowId());
                        taskBuy.scheduleAtFixedRate(() -> {
                            if (!enableBuy) return;
                            api.clientConnection().sendPacket(new Xi(windowId.get(), windowClick.getSlot(), 0, RX.PICKUP, (Vh) ItemStack.of(Item.of(0), 1, 0), (short) 0));
                            if (!forceSingleWindow) windowId.getAndIncrement();
                            api.clientConnection().sendPacket(new XF(abU.MAIN_HAND));
                        }, 0, periodBuy, TimeUnit.MICROSECONDS);
                    } else {
                        enableBuy = false;
                        api.chat().printChatMessage(Text.of("[BetterCSC] - Быстрая покупка ", TextFormatting.GOLD, "выключена", TextFormatting.RED));
                        if (taskBuy != null) taskBuy.shutdown();
                        taskBuy = null;
                    }
                }
                if (KeyboardHelper.isCtrlKeyDown() && windowClick.getMouseButton() == 2) {
                    api.chat().printChatMessage(Text.of("[BetterCSC] - ", TextFormatting.GOLD, "Выходим из игры", TextFormatting.RED));
                    api.chat().sendChatMessage("/hub");
//                  api.clientConnection().sendPayload("csc:sendlobby", Unpooled.buffer());
                }
            }
        }, 100);
        ScreenDisplay.BUS.register(this, screenDisplay -> {
            if (enableUP && screenDisplay.getScreen() != null && screenDisplay.getScreen().doesGuiPauseGame())
                screenDisplay.setCancelled(true);
            if (enableBuy && screenDisplay.getScreen() == null) {
                enableBuy = false;
                api.chat().printChatMessage(Text.of("[BetterCSC] - Кажется вы закрыли GUI, быстрая покупка ", TextFormatting.GOLD, "выключена", TextFormatting.RED));
                if (taskBuy != null) taskBuy.shutdown();
                taskBuy = null;
            }
            if (screenDisplay.getScreen() instanceof asO) {
                screenDisplay.setCancelled(true);
                api.minecraft().displayScreen(new us());
            }
        }, 100);
    }

    @Override
    public void unload() {
        this.hp = false;
        HungerRender.BUS.unregisterAll(this);
        HealthRender.BUS.unregisterAll(this);
        ChatSend.BUS.unregisterAll(this);
        ChatReceive.BUS.unregisterAll(this);
        ArmorRender.BUS.unregisterAll(this);
        PlayerListRender.BUS.unregisterAll(this);
        PluginMessage.BUS.unregisterAll(this);
        WindowClick.BUS.unregisterAll(this);
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