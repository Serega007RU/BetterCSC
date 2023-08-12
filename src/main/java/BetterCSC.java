import com.google.gson.Gson;
import dev.xdark.clientapi.entity.EntityPlayerSP;
import dev.xdark.clientapi.event.chat.ChatReceive;
import dev.xdark.clientapi.event.gui.ScreenDisplay;
import dev.xdark.clientapi.event.inventory.WindowClick;
import dev.xdark.clientapi.event.network.PluginMessage;
import dev.xdark.clientapi.event.render.*;
import dev.xdark.clientapi.event.chat.ChatSend;
import dev.xdark.clientapi.gui.Screen;
import dev.xdark.clientapi.input.KeyboardHelper;
import dev.xdark.clientapi.inventory.ClickType;
import dev.xdark.clientapi.item.Item;
import dev.xdark.clientapi.item.ItemStack;
import dev.xdark.clientapi.network.NetworkPlayerInfo;
import dev.xdark.clientapi.text.Text;
import dev.xdark.clientapi.text.TextFormatting;
import dev.xdark.clientapi.ClientApi;
import dev.xdark.clientapi.event.Listener;
import dev.xdark.clientapi.entry.ModMain;
import dev.xdark.feder.NetUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import leaderboards.BoardContent;
import leaderboards.BoardStructure;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
public class BetterCSC implements ModMain, Listener {
    private boolean hp = true;

    //Скрытие флуда в чате при быстрой прокачке/покупке
    private Text lastMessage = null;
    private long lastMessageTimeMillis;

    //Подсчёт общей суммы ставок
    private long allBets = 0;
    private boolean countBets = false;

    //Быстрая прокачка меча без лагов
    private boolean enableUP = false;
    private ScheduledExecutorService taskUP = null;
    private int periodUP = 6666;
    private int countUp = 0;

    //Быстрая покупка книг с автоюзанием
    private boolean enableBuy = false;
    private ScheduledExecutorService taskBuy = null;
    private int periodBuy = 40000;
    private boolean forceSingleWindow = false;

    //Автоставки
//    private boolean doAutoBet = false;

    //Оповещение в чате когда кто-то заходит/выходит в катке CSC
    private Collection<NetworkPlayerInfo> playerList;
    private boolean startGame = false;

    //Таблица топ рейтинга
    private final Gson gson = new Gson();
    private BoardStructure boardStructure;
    private BoardContent boardContent;
    private boolean reset = false;

    //Префикс мода
    private final Text prefix = Text.of("[", TextFormatting.DARK_RED, "BetterCSC", TextFormatting.DARK_PURPLE, "]", TextFormatting.DARK_RED, " ");

    @Override
    public void load(ClientApi api) {
        api.chat().printChatMessage(prefix.copy().append(Text.of("Plus Edition", TextFormatting.DARK_AQUA, " загружен, by ", TextFormatting.GOLD, "Serega007", TextFormatting.DARK_GREEN, " & ", TextFormatting.GOLD, "VVHIX", TextFormatting.DARK_GREEN)));
//        if (api.minecraft().currentScreen() instanceof asO) api.minecraft().displayScreen(new us());
        ChatSend.BUS.register(this, chatSend -> {
            if (chatSend.isCommand()) {
                String msg = chatSend.getMessage().toLowerCase();

                if (msg.startsWith("/hp")) {
                    chatSend.setCancelled(true);
                    if (this.hp)
                        api.chat().printChatMessage(prefix.copy().append(Text.of("отключён", TextFormatting.RED)));
                    else
                        api.chat().printChatMessage(prefix.copy().append(Text.of("включён", TextFormatting.GREEN)));
                    this.hp = !this.hp;
                } else if (msg.startsWith("/up")) {
                    chatSend.setCancelled(true);
                    if (enableUP) {
                        enableUP = false;
                        api.chat().printChatMessage(prefix.copy().append(Text.of("Быстрый апгрейд ", TextFormatting.GOLD, "выключен", TextFormatting.RED)));
                        if (taskUP != null) taskUP.shutdown();
                        taskUP = null;
                        countUp = 0;
                        return;
                    }
                    int count;
                    try {
                        count = Integer.parseInt(msg.replace("/up ", ""));
                        if (count > 500) {
                            api.chat().printChatMessage(prefix.copy().append(Text.of("Куда так много? Максимум можно 500", TextFormatting.RED)));
                            return;
                        }
                        if (count < 0) throw new RuntimeException();
                    } catch (Exception e) {
                        api.chat().printChatMessage(prefix.copy().append(Text.of("Укажите число", TextFormatting.RED)));
                        return;
                    }
                    EntityPlayerSP player = api.minecraft().getPlayer();
                    try {
                        player.getInventory().getCurrentItem().getItem().getId();
                    } catch (Exception e) {
                        api.chat().printChatMessage(prefix.copy().append(Text.of("В руках отсутствует предмет", TextFormatting.RED)));
                        return;
                    }
                    fireChatSend("/mod unload CSC mod");
                    api.chat().printChatMessage(prefix.copy().append(Text.of("Быстрый апгрейд ", TextFormatting.GOLD, "включён", TextFormatting.GREEN)));
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
                            api.chat().printChatMessage(prefix.copy().append(Text.of("Быстрый апгрейд ", TextFormatting.GOLD, "выключен", TextFormatting.RED, ", ", TextFormatting.GOLD, "достигли заданного числа", TextFormatting.GREEN)));
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
                } else if (msg.startsWith("/period up")) {
                    chatSend.setCancelled(true);
                    int period;
                    try {
                        period = Integer.parseInt(msg.replace("/period up ", ""));
                        if (period > 150) {//Так как сосаликс ложится от такой нагрузки, поэтому такая защита от ддоса
                            api.chat().printChatMessage(prefix.copy().append(Text.of("Куда ты так торопишься? Максимум можно 150", TextFormatting.RED)));
                            return;
                        }
                        if (period < 0) throw new RuntimeException();
                        periodUP = 1000000 / period;
                    } catch (Exception e) {
                        api.chat().printChatMessage(prefix.copy().append(Text.of("Укажите число", TextFormatting.RED)));
                        return;
                    }
                    api.chat().printChatMessage(prefix.copy().append(Text.of("Период прокачки настроен на ", TextFormatting.GOLD, String.valueOf(period), TextFormatting.WHITE)));
                } else if (msg.startsWith("/period buy")) {
                    chatSend.setCancelled(true);
                    int period;
                    try {
                        period = Integer.parseInt(msg.replace("/period buy ", ""));
                        if (period > 35) {
                            api.chat().printChatMessage(prefix.copy().append(Text.of("Куда ты так торопишься? Максимум можно 35", TextFormatting.RED)));
                            return;
                        }
                        if (period < 0) throw new RuntimeException();
                        periodBuy = 1000000 / period;
                    } catch (Exception e) {
                        api.chat().printChatMessage(prefix.copy().append(Text.of("Укажите число", TextFormatting.RED)));
                        return;
                    }
                    api.chat().printChatMessage(prefix.copy().append(Text.of("Период покупки настроен на ", TextFormatting.GOLD, String.valueOf(period), TextFormatting.WHITE)));
                } else if (msg.startsWith("/forcesingleup")) {
                    chatSend.setCancelled(true);
                    forceSingleWindow = !forceSingleWindow;
                    if (forceSingleWindow) api.chat().printChatMessage(prefix.copy().append(Text.of("ForceSingleUp ", TextFormatting.GOLD, "включён", TextFormatting.WHITE)));
                    else api.chat().printChatMessage(prefix.copy().append(Text.of("ForceSingleUp ", TextFormatting.GOLD, "выключен", TextFormatting.RED)));

                } else if (msg.startsWith("/sendpayload ")) {
                    chatSend.setCancelled(true);
                    api.clientConnection().sendPayload(msg.replace("/sendpayload ", ""), Unpooled.buffer());
                } else if (msg.startsWith("/mod ")) {
                    chatSend.setCancelled(true);
                    try {
                        aws aws;
                        tx tx;
                        jr jr;
                        List<String> mods = new ArrayList<>();
                        //Получаем доступ к приватному классу ClientApi наследующий класс c
                        {
                            Field field = null;
                            for (Field f : api.getClass().getDeclaredFields()) {
                                if (f.getName().equals("a") && f.getType().getName().equals("dev.xdark.clientapi.ClientApi")) {
                                    field = f;
                                    break;
                                }
                            }
                            field.setAccessible(true);
                            jr = (jr) field.get(api);
                        }

                        //Для начала достаём класс Minecraft (Minecraft.getMinecraft())
                        {
                            Field field = null;
                            for (Field f : jr.getClass().getDeclaredFields()) {
                                if (f.getName().equals("a") && f.getType().getName().equals("tx")) {
                                    field = f;
                                    break;
                                }
                            }
                            field.setAccessible(true);
                            tx = (tx) field.get(jr);
                        }

                        //Получаем доступ к классу aws в котором хранится список модов
                        {
                            Field field = null;
                            for (Field f : tx.getClass().getDeclaredFields()) {
                                if (f.getName().equals("as")) {
                                    field = f;
                                    break;
                                }
                            }
                            field.setAccessible(true);
                            aws = (aws) field.get(tx);
                        }

                        Map<String, ModMain> modList;
                        //Получаем доступ к списку модов и делаем чо хотим с модом
                        {
                            Field field = null;
                            for (Field f : aws.class.getDeclaredFields()) {
                                if (f.getName().equals("de")) {
                                    field = f;
                                    break;
                                }
                            }
                            field.setAccessible(true);
                            //noinspection unchecked
                            modList = (Map<String, ModMain>) field.get(aws);
                        }
                        //Получаем доступ ко второму списку модов и делаем чо хотим с модом
                        {
                            Field field = null;
                            for (Field f : aws.class.getDeclaredFields()) {
                                if (f.getName().equals("df")) {
                                    field = f;
                                    break;
                                }
                            }
                            field.setAccessible(true);
                            //noinspection unchecked
                            modList.putAll((Map<String, ModMain>) field.get(aws));
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
                                    api.chat().printChatMessage(prefix.copy().append(Text.of("Выгружаем ", TextFormatting.RED, mod.getKey(), TextFormatting.WHITE)));
                                } else {
                                    api.chat().printChatMessage(prefix.copy().append(Text.of("Загружаем ", TextFormatting.GREEN, mod.getKey(), TextFormatting.WHITE)));
                                }
//                                hw hw = (hw) mod.getValue();
//                                ModMain modMain = null;
//                                try {
//                                    Field field = null;
//                                    for (Field f : hw.class.getDeclaredFields()) {
//                                        if (f.getName().equals("a") && f.getType().getName().equals("dev.xdark.clientapi.entry.ModMain")) {
//                                            field = f;
//                                            break;
//                                        }
//                                    }
//                                    field.setAccessible(true);
//                                    modMain = (ModMain) field.get(hw);
//                                } catch (Exception e) {
//                                    e.printStackTrace();
//                                    api.chat().printChatMessage(Text.of(e.getLocalizedMessage(), TextFormatting.DARK_RED));
//                                }
//                                if (msg.startsWith("/mod unload ")) {
//                                    modMain.unload();
//                                    if (modMain instanceof Listener) {
//                                        ChatSend.BUS.unregisterAll((Listener) modMain);
//                                    }
//                                } else {
//                                    modMain.load(clientApi);
//                                }
                                if (msg.startsWith("/mod unload ")) {
                                    mod.getValue().unload();
                                    if (mod.getValue() instanceof Listener) {
                                        ChatSend.BUS.unregisterAll((Listener) mod.getValue());
                                    }
                                } else {
                                    mod.getValue().load(jr);
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
//                        for (Field f : aws.class.getFields()) {
//                            if (f.getName().equals("a") && f.getType().getName().equals("java.util.Collection")) {
//                                field = f;
//                                break;
//                            }
//                        }
//                        //noinspection unchecked
//                        Collection<ModMain> modList = (Collection<ModMain>) field.get(aws);
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
                } else if (msg.startsWith("/unloadbcsc")) {
                    chatSend.setCancelled(true);
                    api.chat().printChatMessage(prefix.copy().append(Text.of("Выгружаем данный мод, пока =(", TextFormatting.WHITE)));
                    unload();
                } else if (msg.startsWith("/leadertop")) {
                    chatSend.setCancelled(true);
                    if (boardContent != null) {
                        api.chat().printChatMessage(Text.of("Топ рейтинга: ", TextFormatting.YELLOW));
                        for (BoardContent.BoardLine line : boardContent.getContent()) {
                            Text text = Text.of("");
                            for (String column : line.getColumns()) {
                                text.append(stringToText(column + " "));
                            }
                            api.chat().printChatMessage(text);
                        }

                    } else {
                        api.chat().printChatMessage(prefix.copy().append(Text.of("Нет таблицы", TextFormatting.RED)));
                    }
                }
            }
        }, 100);

        ChatReceive.BUS.register(this, chatReceive -> {
            String msg = chatReceive.getText().getUnformattedText();
            String msgColored = chatReceive.getText().getFormattedText();

            if (this.hp) {

                if (enableUP) {
                    if (msgColored.contains("§aБаланс: ") || msgColored.contains("§aВы успешно улучшили предмет")) {
                        chatReceive.setCancelled(true);
                        return;
                    } else if (msgColored.contains("§cУ вас недостаточно золота на балансе") || msgColored.contains("§cЭтот предмет нельзя улучшить") || msgColored.contains("§cВы не находитесь в игре") || msgColored.contains("§cОшибка, вы не можете сейчас открыть меню апгрейда") || msgColored.contains("§cЭтот предмет улучшен до максимального уровня")) {
                        chatReceive.setCancelled(true);
                        enableUP = false;
                        if (taskUP != null) taskUP.shutdown();
                        api.chat().printChatMessage(prefix.copy().append(Text.of("Быстрый апгрейд ", TextFormatting.GOLD, "выключен", TextFormatting.RED, ", " + TextFormatting.GOLD, msg, TextFormatting.RED)));
                        return;
                    }
                } else if (enableBuy) {
                    if (msgColored.contains("§aБаланс: ") || msgColored.contains("§aВы успешно купили предмет")) {
                        chatReceive.setCancelled(true);
                        return;
                    } else if (msgColored.contains("§cУ вас недостаточно золота на балансе") || msgColored.contains("§cВы уже купили этот предмет") || msgColored.contains("§cУ вас недостаточно места в инвентаре")) {
                        chatReceive.setCancelled(true);
                        enableBuy = false;
                        if (taskBuy != null) taskBuy.shutdown();
                        api.chat().printChatMessage(prefix.copy().append(Text.of("Быстрая покупка ", TextFormatting.GOLD, "выключена", TextFormatting.RED, ", ", TextFormatting.GOLD, msg, TextFormatting.RED)));
                        return;
                    }
                }

                if (msgColored.contains("§aБаланс: ")) {
                    chatReceive.setCancelled(true);
                    if (lastMessage != null) {
                        api.chat().printChatMessage(lastMessage);
                    }
                    String text = chatReceive.getText().getFormattedText();
                    String[] list = msg.replaceAll("[^-?0-9]+", " ").trim().split(" ");
                    for (String str : list) {
                        long num = Long.parseLong(str);
                        text = text.replaceAll(str, new DecimalFormat("#,###").format(num));
                    }
                    chatReceive.setText(stringToText(text));
                    lastMessage = chatReceive.getText();
                    lastMessageTimeMillis = System.currentTimeMillis();
                } else if (msgColored.contains("§aВы успешно улучшили предмет") || msgColored.contains("§aВы успешно купили предмет")) {
                    chatReceive.setCancelled(true);
                    lastMessage = null;
                } else if (msgColored.contains("§aСтавки выиграли:")) {
                    countBets = true;
                } else if (countBets) {
                    if (msgColored.contains("§7- ")) {
                        String text = msg;
                        text = msg.substring(text.indexOf(" - ") + 3, text.length());
                        long num = Long.parseLong(text);
                        allBets += num;

                        String unfText = chatReceive.getText().getFormattedText();
                        unfText = unfText.replaceAll(String.valueOf(num), new DecimalFormat("#,###").format(num));
                        chatReceive.setText(stringToText(unfText));
                    } else {
                        api.chat().printChatMessage(Text.of("Общая сумма ставок: ", TextFormatting.GOLD, new DecimalFormat("#,###").format(allBets), TextFormatting.GOLD));
                        countBets = false;
                        allBets = 0;
                    }
                } else if (msgColored.contains("§aсорвал куш и получил ") || msgColored.contains("§aсорвала джекпот и получила ")) {
                    String var;
                    if (msg.contains(" сорвал куш и получил ")) {
                        var = " сорвал куш и получил ";
                    } else {
                        var = " сорвала джекпот и получила ";
                    }
                    String text = chatReceive.getText().getFormattedText();
                    long num = Long.parseLong(msg.substring(msg.indexOf(var) + var.length(), msg.indexOf(" золота")));
                    text = text.replaceAll(String.valueOf(num), new DecimalFormat("#,###").format(num));
                    chatReceive.setText(stringToText(text));
                }

//                if (lastMessage != null) {
//                    api.chat().printChatMessage(lastMessage);
//                    lastMessage = null;
//                }
            }
        }, 100);

        ScheduledExecutorService task = api.threadManagement().newSingleThreadedScheduledExecutor();
        task.scheduleAtFixedRate(() -> {
            if (lastMessage != null) {
                if (lastMessageTimeMillis + 1000 < System.currentTimeMillis()) {
                    api.chat().printChatMessage(lastMessage);
                    lastMessage = null;
                }
            }
        }, 0, 1, TimeUnit.SECONDS);

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
//                if (KeyboardHelper.isAltKeyDown() && windowClick.getMouseButton() == 2 && windowClick.getClickType() == ClickType.CLONE) {
//                    if (!enableBuy) {
//                        api.chat().printChatMessage(prefix.copy().append(Text.of("Быстрая покупка ", TextFormatting.GOLD, "включена", TextFormatting.GREEN)));
//                        enableBuy = true;
//
//                        taskBuy = api.threadManagement().newSingleThreadedScheduledExecutor();
//                        AtomicInteger windowId = new AtomicInteger(windowClick.getWindowId());
//                        taskBuy.scheduleAtFixedRate(() -> {
//                            if (!enableBuy) return;
//                            try {
//                                sendPacket(api, new XO(windowId.get(), windowClick.getSlot(), 0, SD.PICKUP, (VN) ItemStack.of(Item.of(0), 1, 0), (short) 0));
//                            } catch (Exception e) {
//                                enableBuy = false;
//                                e.printStackTrace();
//                                api.chat().printChatMessage(Text.of(e.getLocalizedMessage(), TextFormatting.DARK_RED));
//                            }
//                            if (!forceSingleWindow) windowId.getAndIncrement();
////                            api.clientConnection().sendPacket(new XF(abU.MAIN_HAND));
//                        }, 0, periodBuy, TimeUnit.MICROSECONDS);
//                    } else {
//                        enableBuy = false;
//                        api.chat().printChatMessage(prefix.copy().append(Text.of("Быстрая покупка ", TextFormatting.GOLD, "выключена", TextFormatting.RED)));
//                        if (taskBuy != null) taskBuy.shutdown();
//                        taskBuy = null;
//                    }
//                }
//                if (KeyboardHelper.isCtrlKeyDown() && windowClick.getMouseButton() == 2 && windowClick.getClickType() == ClickType.CLONE) {
//                    api.chat().printChatMessage(prefix.copy().append(Text.of("Выходим из игры", TextFormatting.RED)));
//                    api.chat().sendChatMessage("/hub");
////                  api.clientConnection().sendPayload("csc:sendlobby", Unpooled.buffer());
//                }
//                if (KeyboardHelper.isShiftKeyDown() && windowClick.getMouseButton() == 2 && windowClick.getClickType() == ClickType.CLONE) {
//                    api.chat().printChatMessage(prefix.copy().append(Text.of("test", TextFormatting.GREEN)));
//                    doAutoBet = true;
////                    api.chat().sendChatMessage("/duelsettings");
//                }
            }
        }, 100);
        ScreenDisplay.BUS.register(this, screenDisplay -> {
            Screen screen = screenDisplay.getScreen();
            if (enableUP && screen != null && screen.doesGuiPauseGame())
                screenDisplay.setCancelled(true);
            if (enableBuy && screen == null) {
                enableBuy = false;
                api.chat().printChatMessage(prefix.copy().append(Text.of("Кажется вы закрыли GUI, быстрая покупка ", TextFormatting.GOLD, "выключена", TextFormatting.RED)));
                if (taskBuy != null) taskBuy.shutdown();
                taskBuy = null;
            }
//            if (screen instanceof asO) {
//                screenDisplay.setCancelled(true);
//                api.minecraft().displayScreen(new us());
//            }

//            if (doAutoBet) {
//                try {
//                    if (screen instanceof vN) {
//                        //Получаем доступ к приватной переменной обозначающее название открытого Container
//                        vN inv = (vN) screen;
//                        SE se = (SE) inv.getClass().getField("a").get(inv);
//                        //Достаём windowId
//                        vO vo = (vO) screen;
//                        RY ry = (RY) Arrays.stream(vo.getClass().getFields()).filter(field -> field.getName().equals("a") && field.getType().getName().equals("RY")).findFirst().get().get(vo);
//                        int windowId = (int) Arrays.stream(ry.getClass().getFields()).filter(field -> field.getName().equals("a") && field.getType().getName().equals("int")).findFirst().get().get(ry);
//
//                        String text = se.a().getUnformattedText();
//                        api.chat().printChatMessage(Text.of(text));
//                        if (text.equals("Выбрать команду") || text.equals("Выбор игрока")) {
//                            api.chat().printChatMessage(Text.of("screen " + windowId));
//                            api.clientConnection().sendPacket(new Xi(windowId, 11, 0, RX.PICKUP, (Vh) ItemStack.of(Item.of(0), 1, 0), (short) 0));
//                        } else if (text.equals("Ставка на команду")) {
//                            api.chat().printChatMessage(Text.of("Отсылаем с"));
//                            api.clientConnection().sendPacket(new Xi(windowId, 8, 0, RX.PICKUP, (Vh) ItemStack.of(Item.of(0), 1, 0), (short) 0));
//                            api.minecraft().getPlayer().closeScreen();
//                            doAutoBet = false;
//                        }
//
//                    }
//                } catch (Exception e) {
//                    api.chat().printChatMessage(Text.of(e.getLocalizedMessage(), TextFormatting.DARK_RED));
//                    e.printStackTrace();
//                }
//            }
        }, 100);

        PluginMessage.BUS.register(this, pluginMessage -> {
//            System.out.println("Channel:" + pluginMessage.getChannel());
//            System.out.println("Datacha: " + NetUtil.readUtf8(pluginMessage.getData().copy()));
            if (reset && boardStructure != null && boardContent != null) {
                reset = false;
                ByteBuf byteBuf = Unpooled.buffer();
                NetUtil.writeUtf8(gson.toJson(boardStructure), byteBuf);
                firePluginMessage("boards:new", byteBuf);
                ByteBuf byteBuf2 = Unpooled.buffer();
                NetUtil.writeUtf8(gson.toJson(boardContent), byteBuf2);
                firePluginMessage("boards:content", byteBuf2);
            }
            if (pluginMessage.getChannel().equals("csc:updateonline")) {
                startGame = true;
            } else if (pluginMessage.getChannel().equals("csc:tab")) {
                startGame = false;
            } else if (pluginMessage.getChannel().equals("boards:new")) {
                String var4 = NetUtil.readUtf8(pluginMessage.getData().copy(), Integer.MAX_VALUE);
                BoardStructure var6 = gson.fromJson(var4, BoardStructure.class);
                if (var6.getName().equals("§e§lТоп рейтинга")) {
                    //Изменяем координаты только для таблица которая используется
//                    if (var6.getX() == 0) {
                        //Намеренно изменяем координаты board
                        var6.setX(269);
                        var6.setY(108);
                        var6.setZ(-17);
                        var6.setYaw(180);
                        //Изменяем ByteBuf
                        pluginMessage.getData().clear();
                        NetUtil.writeUtf8(gson.toJson(var6), pluginMessage.getData());
//                    }
                    boardStructure = var6;
                }
            } else if (pluginMessage.getChannel().equals("boards:content")) {
                String var4 = NetUtil.readUtf8(pluginMessage.getData().copy(), Integer.MAX_VALUE);
                BoardContent var5 = gson.fromJson(var4, BoardContent.class);
                if (boardStructure != null && var5.getBoardId().toString().equals(boardStructure.getUuid().toString())) {
                    boardContent = var5;
                }
            } else if (pluginMessage.getChannel().equals("boards:reset")) {
                if (boardStructure != null && boardContent != null) reset = true;
            } else if (pluginMessage.getChannel().equals("REGISTER")) {
                playerList = new ArrayList<>(api.clientConnection().getPlayerInfos());
            } else if (pluginMessage.getChannel().equals("csc:ui") && !startGame) {
                String cscUi = NetUtil.readUtf8(pluginMessage.getData().copy(), Integer.MAX_VALUE);
                if (cscUi.startsWith("7:")) {
                    //Да тут просто трилион костылей что бы на этом говно API Cristalix хоть как-то работало
                    if (playerList == null || playerList.size() == 0 || playerList.size() == 1) {
                        playerList = new ArrayList<>(api.clientConnection().getPlayerInfos());
                    }
                    for (NetworkPlayerInfo player1 : api.clientConnection().getPlayerInfos()) {
                        boolean has = false;
                        for (NetworkPlayerInfo player2 : playerList) {
                            if (player1.getGameProfile().getId().toString().equals(player2.getGameProfile().getId().toString())) {
                                has = true;
                                break;
                            }
                        }
                        if (!has && player1 != null && player1.getDisplayName() != null && player1.getDisplayName().getFormattedText() != null && player1.getDisplayName().getFormattedText().length() > 0) {
                            api.chat().printChatMessage(prefix.copy().append(stringToText(player1.getDisplayName().getFormattedText().substring(11, player1.getDisplayName().getFormattedText().length() - (player1.getDisplayName().getFormattedText().contains("[§") ? 0 : 5)))).append(Text.of(" зашёл на сервер", TextFormatting.YELLOW)));
                        }
                    }
                    for (NetworkPlayerInfo player1 : playerList) {
                        boolean has = false;
                        for (NetworkPlayerInfo player2 : api.clientConnection().getPlayerInfos()) {
                            if (player1.getGameProfile().getId().toString().equals(player2.getGameProfile().getId().toString())) {
                                has = true;
                                break;
                            }
                        }
                        if (!has && player1 != null && player1.getDisplayName() != null && player1.getDisplayName().getFormattedText() != null && player1.getDisplayName().getFormattedText().length() > 0) {
                            api.chat().printChatMessage(prefix.copy().append(stringToText(player1.getDisplayName().getFormattedText().substring(11, player1.getDisplayName().getFormattedText().length() - (player1.getDisplayName().getFormattedText().contains("[§") ? 0 : 5)))).append(Text.of(" вышел с сервера", TextFormatting.YELLOW)));
                        }
                    }
                    playerList = new ArrayList<>(api.clientConnection().getPlayerInfos());
                }
            }
        }, 1);
    }

    @SuppressWarnings("UnusedReturnValue")
    public ChatSend fireChatSend(String message) {
        return ChatSend.BUS.fire(new ChatSend() {
            boolean canceled = false;
            String message_ = message;

            @Override
            public String getMessage() {
                return message_;
            }

            @Override
            public void setMessage(String message) {
                this.message_ = message;
            }

            @Override
            public boolean isCommand() {
                return message_.startsWith("/");
            }

            @Override
            public boolean isCancelled() {
                return canceled;
            }

            @Override
            public void setCancelled(boolean cancelled) {
                this.canceled = cancelled;
            }
        });
    }

    @SuppressWarnings("UnusedReturnValue")
    public PluginMessage firePluginMessage(String channel, ByteBuf byteBuf) {
        return PluginMessage.BUS.fire(new PluginMessage() {
            @Override
            public String getChannel() {
                return channel;
            }

            @Override
            public ByteBuf getData() {
                return byteBuf;
            }
        });
    }

    @Override
    public void unload() {
        this.hp = false;
        ChatSend.BUS.unregisterAll(this);
    }

    private int getColor(int percent) {
        int red = (int) ((percent > 50 ? 1 - 2 * (percent - 50) / 100.0 : 1.0) * 255);
        int green = (int) ((percent > 50 ? 1.0 : 2 * percent / 100.0) * 255);
//      int blue = 0;
        red = (red << 16) & 0x00FF0000; //Shift red 16-bits and mask out other stuff
        green = (green << 8) & 0x0000FF00; //Shift Green 8-bits and mask out other stuff
        int blue = 0/* & 0x000000FF*/; //Mask out anything not blue.

        return 0xFF000000 | red | green | blue; //0xFF000000 for 100% Alpha. Bitwise OR everything together.
    }

    private final Map<Character, TextFormatting> textFormattingValues = new HashMap<Character, TextFormatting>() {{
        put('0', TextFormatting.BLACK);
        put('1', TextFormatting.DARK_BLUE);
        put('2', TextFormatting.DARK_GREEN);
        put('3', TextFormatting.DARK_AQUA);
        put('4', TextFormatting.DARK_RED);
        put('5', TextFormatting.DARK_PURPLE);
        put('6', TextFormatting.GOLD);
        put('7', TextFormatting.GRAY);
        put('8', TextFormatting.DARK_GRAY);
        put('9', TextFormatting.BLUE);
        put('a', TextFormatting.GREEN);
        put('b', TextFormatting.AQUA);
        put('c', TextFormatting.RED);
        put('d', TextFormatting.LIGHT_PURPLE);
        put('e', TextFormatting.YELLOW);
        put('f', TextFormatting.WHITE);
        put('k', TextFormatting.OBFUSCATED);
        put('l', TextFormatting.BOLD);
        put('m', TextFormatting.STRIKETHROUGH);
        put('n', TextFormatting.UNDERLINE);
        put('o', TextFormatting.ITALIC);
        put('r', TextFormatting.RESET);
    }};

    //Что бы адекватно в консоль выводился текст (без кода цвета)
    private Text stringToText(String text) {
        String[] list = text.replaceAll("§", "regex").split("regex");
        Text formText = Text.of("");
        for (String str : list) {
            if (str.length() == 0) continue;
            char ch = str.charAt(0);
            TextFormatting textFormatting = textFormattingValues.get(ch);
            if (textFormatting == null) {
                formText.append(Text.of(str));
            } else if (textFormatting == TextFormatting.OBFUSCATED || textFormatting == TextFormatting.BOLD || textFormatting == TextFormatting.STRIKETHROUGH || textFormatting == TextFormatting.UNDERLINE) {
                formText.append(Text.of(textFormatting));
            } else {
                str = str.substring(1);
                formText.append(Text.of(str, textFormatting));
            }
        }
        return formText;
    }

    public Method sendPacketMethod;
    public zu sendPacketInstance;
    public void sendPacket(ClientApi api, WZ packet) throws InvocationTargetException, IllegalAccessException {
        if (sendPacketMethod == null) {
            // Сначало получаем доступ к zu
            zu zu;
            {
                Method method = null;
                for (Method m : ((sB) api.clientConnection()).getClass().getDeclaredMethods()) {
                    if (m.getName().equals("a") && m.getReturnType().getName().equals("WU")) {
                        method = m;
                        break;
                    }
                }
                method.setAccessible(true);
                zu = (zu) method.invoke(api);
                sendPacketInstance = zu;
            }
            //Позже получаем метод который вызывает sendPacket
            {
                Method method = null;
                for (Method m : zu.getClass().getDeclaredMethods()) {
                    if (m.getName().equals("a")) {
                        for (Class<?> class_ : m.getParameterTypes()) {
                            if (class_.getName().equals("WZ")) {
                                method = m;
                                break;
                            }
                        }
                        if (method != null) {
                            break;
                        }
                    }
                }
                method.setAccessible(true);
                sendPacketMethod = method;
            }
        }
        sendPacketMethod.invoke(sendPacketInstance, packet);
    }
}