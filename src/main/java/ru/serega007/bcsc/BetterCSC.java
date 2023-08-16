package ru.serega007.bcsc;

import com.xenoceal.cristalix.Reflection;
import com.xenoceal.cristalix.Wrapper;
import dev.xdark.clientapi.entity.EntityPlayerSP;
import dev.xdark.clientapi.event.chat.ChatReceive;
import dev.xdark.clientapi.event.network.PluginMessage;
import dev.xdark.clientapi.event.network.ServerSwitch;
import dev.xdark.clientapi.event.render.*;
import dev.xdark.clientapi.event.chat.ChatSend;
import dev.xdark.clientapi.network.NetworkPlayerInfo;
import dev.xdark.clientapi.text.Text;
import dev.xdark.clientapi.text.TextFormatting;
import dev.xdark.clientapi.ClientApi;
import dev.xdark.clientapi.event.Listener;
import dev.xdark.clientapi.entry.ModMain;
import dev.xdark.feder.NetUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    private boolean protectUp = false;
    private ScheduledExecutorService taskUP = null;
    private int periodUP = 6666;
    private int countUp = 0;

    //Быстрая покупка книг с автоюзанием
    private boolean enableBuy = false;
    private boolean protectBuy = false;
    private ScheduledExecutorService taskBuy = null;
    private int periodBuy = 40000;
    private int countBuy = 0;
    private String uuidWindowBuy = null;

    //Автоставки
//    private boolean doAutoBet = false;

    //Оповещение в чате когда кто-то заходит/выходит в катке CSC
    private Collection<NetworkPlayerInfo> playerList;
    private boolean startGame = false;


    //Префикс мода
    private final Text prefix = Text.of("[", TextFormatting.DARK_RED, "BetterCSC", TextFormatting.DARK_PURPLE, "]", TextFormatting.DARK_RED, " ");

    @Override
    public void load(ClientApi api) {
        try {
            Reflection.initialize();
        } catch (Exception e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            api.chat().printChatMessage(prefix.copy().append(Text.of("Ошибка загрузки маппингов, обратитесь к разработкичку https://gitlab.com/Serega007/bettercsc/-/issues " + e.getLocalizedMessage(), TextFormatting.DARK_RED)));
            return;
        }

        api.chat().printChatMessage(prefix.copy().append(Text.of("Plus Edition", TextFormatting.DARK_AQUA, " версии ", TextFormatting.GOLD, "2.5.5", TextFormatting.YELLOW, " загружен, by ", TextFormatting.GOLD, "Serega007", TextFormatting.DARK_GREEN, " & ", TextFormatting.GOLD, "VVHIX", TextFormatting.DARK_GREEN)));
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
                        disableUp(api, Text.of(""));
                        return;
                    }
                    if (protectUp) {
                        api.chat().printChatMessage(prefix.copy().append(Text.of("Быстрый апгрейд пока ещё не полнолстью выключился, подождите где-то 5 секунд", TextFormatting.RED)));
                        return;
                    }
                    int count;
                    try {
                        count = Integer.parseInt(msg.replace("/up ", ""));
                        if (count > 5000) {
                            api.chat().printChatMessage(prefix.copy().append(Text.of("Куда так много? Максимум можно 5000", TextFormatting.RED)));
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
                    api.chat().printChatMessage(prefix.copy().append(Text.of("Быстрый апгрейд ", TextFormatting.GOLD, "включён", TextFormatting.GREEN)));

                    enableUP = true;
                    protectUp = true;
                    countUp = 0;

                    int slot = player.getInventory().getActiveSlot();
                    int id = player.getInventory().getCurrentItem().getItem().getId();

                    taskUP = api.threadManagement().newSingleThreadedScheduledExecutor();
                    taskUP.scheduleAtFixedRate(() -> {
                        if (!enableUP) return;
                        countUp++;
                        if (countUp > count) {
                            disableUp(api, Text.of(TextFormatting.RED, ", ", TextFormatting.GOLD, "достигли заданного числа", TextFormatting.GREEN));
                            return;
                        }
                        ByteBuf buffer;
                        ByteBuf $this$writeVarInt$iv = buffer = Unpooled.buffer();
                        NetUtil.writeVarInt(slot, $this$writeVarInt$iv);
                        NetUtil.writeVarInt(id, buffer);
                        api.clientConnection().sendPayload("csc:upgrade", buffer);
                    }, 0, periodUP, TimeUnit.MICROSECONDS);
                } else if (msg.startsWith("/buy")) {
                    chatSend.setCancelled(true);
                    if (enableBuy) {
                        disableBuy(api, Text.of(""));
                        return;
                    }
                    if (protectBuy) {
                        api.chat().printChatMessage(prefix.copy().append(Text.of("Быстрая покупка пока ещё не полнолстью выключилась, подождите где-то 5 секунд", TextFormatting.RED)));
                        return;
                    }
                    int id;
                    int count;
                    try {
                        String[] args = msg.split(" ");
                        count = Integer.parseInt(args[1]);
                        id = Integer.parseInt(args[2]);
                        if (count > 1000) {
                            api.chat().printChatMessage(prefix.copy().append(Text.of("Куда так много? Максимум можно 1000", TextFormatting.RED)));
                            return;
                        }
                        if (id <= 0 || count <= 0) throw new RuntimeException();
                    } catch (Exception e) {
                        api.chat().printChatMessage(prefix.copy().append(Text.of("Неверно указаны числа, ", TextFormatting.RED, "/buy <кол-во> <номер слота>", TextFormatting.GOLD)));
                        return;
                    }

                    if (uuidWindowBuy == null) {
                        api.chat().printChatMessage(prefix.copy().append(Text.of("Похоже вы не открывали меню Магазина, откройте его (хотя бы на секунду) что б мод запомнил ID меню")));
                        return;
                    }

                    api.chat().printChatMessage(prefix.copy().append(Text.of("Быстрая покупка ", TextFormatting.GOLD, "включена", TextFormatting.GREEN)));

                    enableBuy = true;
                    protectBuy = true;
                    countBuy = 0;

                    taskBuy = api.threadManagement().newSingleThreadedScheduledExecutor();
                    taskBuy.scheduleAtFixedRate(() -> {
                        if (!enableBuy || uuidWindowBuy == null) return;
                        countBuy++;
                        if (countBuy > count) {
                            disableBuy(api, Text.of(TextFormatting.RED, ", ", TextFormatting.GOLD, "достигли заданного числа", TextFormatting.GREEN));
                            return;
                        }
                        ByteBuf buffer = Unpooled.buffer();
                        NetUtil.writeUtf8(buffer, uuidWindowBuy);
                        buffer.writeInt(id - 1);
                        buffer.writeInt(0);
                        api.clientConnection().sendPayload("storage:click", buffer);
                        try {
                            Wrapper.rightClickMouse();
                        } catch (Exception e) {
                            //noinspection CallToPrintStackTrace
                            e.printStackTrace();
                            disableUp(api, Text.of(e.getLocalizedMessage(), TextFormatting.DARK_RED));
                        }
                    }, 0, periodBuy, TimeUnit.MICROSECONDS);
                } else if (msg.startsWith("/period")) {
                    if (msg.startsWith("/period up")) {
                        chatSend.setCancelled(true);
                        int period;
                        try {
                            period = Integer.parseInt(msg.replace("/period up ", ""));
                            if (period > 500) {//Так как сосаликс ложится от такой нагрузки, поэтому такая защита от ддоса
                                api.chat().printChatMessage(prefix.copy().append(Text.of("Куда ты так торопишься? Максимум можно 500", TextFormatting.RED)));
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
                            if (period > 500) {
                                api.chat().printChatMessage(prefix.copy().append(Text.of("Куда ты так торопишься? Максимум можно 500", TextFormatting.RED)));
                                return;
                            }
                            if (period < 0) throw new RuntimeException();
                            periodBuy = 1000000 / period;
                        } catch (Exception e) {
                            api.chat().printChatMessage(prefix.copy().append(Text.of("Укажите число", TextFormatting.RED)));
                            return;
                        }
                        api.chat().printChatMessage(prefix.copy().append(Text.of("Период покупки настроен на ", TextFormatting.GOLD, String.valueOf(period), TextFormatting.WHITE)));
                    } else {
                        chatSend.setCancelled(true);
                        api.chat().printChatMessage(prefix.copy().append(Text.of("Неверно указаны агрументы, ", TextFormatting.RED, "/period <buy/up> <число>", TextFormatting.GOLD)));
                    }
                } else if (msg.startsWith("/unloadbcsc")) {
                    chatSend.setCancelled(true);
                    api.chat().printChatMessage(prefix.copy().append(Text.of("Выгружаем данный мод, пока =(", TextFormatting.WHITE)));
                    unload();
                }
            }
        }, 100);

        ChatReceive.BUS.register(this, chatReceive -> {
            String msg = chatReceive.getText().getUnformattedText();
            String msgColored = chatReceive.getText().getFormattedText();

            if (this.hp) {

                if (enableUP || enableBuy || protectUp || protectBuy) {
                    if (msgColored.contains("§aБаланс: ") || msgColored.contains("§aВы успешно улучшили предмет") || msgColored.contains("§aВы успешно купили предмет") || msgColored.contains("§cУ вас максимальное количество денег")) {
                        chatReceive.setCancelled(true);
                        return;
                    } else if (msgColored.contains("§cУ вас недостаточно золота на балансе") || msgColored.contains("§cВы уже купили этот предмет") || msgColored.contains("§cЭтот предмет нельзя улучшить") || msgColored.contains("§cВы не находитесь в игре") || msgColored.contains("§cОшибка, вы не можете сейчас открыть меню апгрейда") || msgColored.contains("§cЭтот предмет улучшен до максимального уровня") || msgColored.contains("§cУ вас недостаточно места в инвентаре") || msgColored.contains("§cВы не можете сейчас покупать предметы")) {
                        chatReceive.setCancelled(true);
                        if (enableUP) {
                            disableUp(api, Text.of(TextFormatting.RED, ", " + TextFormatting.GOLD, msg, TextFormatting.RED));
                        } else if (enableBuy) {
                            disableBuy(api, Text.of(TextFormatting.RED, ", " + TextFormatting.GOLD, msg, TextFormatting.RED));
                        }
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

                api.fontRenderer().drawString(String.format("%s ♥", (int)health),
                        w / 2.0F - 88.0F, h - 50.0f,
                        color, true); // Health

                String mh = String.format("%6s ♥", (int)maxHealth);
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

        ServerSwitch.BUS.register(this, serverSwitch -> uuidWindowBuy = null, 1);

        PluginMessage.BUS.register(this, pluginMessage -> {
//            System.out.println("Channel:" + pluginMessage.getChannel());
//            System.out.println("Datacha: " + NetUtil.readUtf8(pluginMessage.getData().copy()));
            if (pluginMessage.getChannel().equals("csc:updateonline")) {
                startGame = true;
            } else if (pluginMessage.getChannel().equals("csc:tab")) {
                startGame = false;
            } else if (pluginMessage.getChannel().equals("REGISTER")) {
                playerList = new ArrayList<>(api.clientConnection().getPlayerInfos());
            } else if (pluginMessage.getChannel().equals("csc:ui") && !startGame) {
                String cscUi = NetUtil.readUtf8(pluginMessage.getData().copy(), Integer.MAX_VALUE);
                if (cscUi.startsWith("7:")) {
                    //Да тут просто трилион костылей что бы на этом говно API Cristalix хоть как-то работало
                    if (playerList == null || playerList.isEmpty() || playerList.size() == 1) {
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
                        if (!has && player1 != null && player1.getDisplayName() != null && player1.getDisplayName().getFormattedText() != null && !player1.getDisplayName().getFormattedText().isEmpty()) {
                            // Это полный кринж, в displayName мы в начале получаем кракозябры из всяких не понятных символов, говно API дристаликса
                            String nick = player1.getDisplayName().getFormattedText().substring(player1.getDisplayName().getFormattedText().indexOf("§r§f") + 5);
                            if (nick.startsWith("§r§f ")) nick = nick.substring(5);
                            api.chat().printChatMessage(prefix.copy().append(stringToText(nick).append(Text.of(" зашёл в игру", TextFormatting.GREEN))));
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
                        if (!has && player1 != null && player1.getDisplayName() != null && player1.getDisplayName().getFormattedText() != null && !player1.getDisplayName().getFormattedText().isEmpty()) {
                            String nick = player1.getDisplayName().getFormattedText().substring(player1.getDisplayName().getFormattedText().indexOf("§r§f") + 5);
                            if (nick.startsWith("§r§f ")) nick = nick.substring(5);
                            api.chat().printChatMessage(prefix.copy().append(stringToText(nick).append(Text.of(" покинул игру", TextFormatting.RED))));
                        }
                    }
                    playerList = new ArrayList<>(api.clientConnection().getPlayerInfos());
                }
            } else if (pluginMessage.getChannel().equals("func:page-response")) {
                uuidWindowBuy = NetUtil.readUtf8(pluginMessage.getData().copy());
            } else if (pluginMessage.getChannel().equals("csc:upgrade")) {
                if (protectUp) {
                    pluginMessage.getData().clear();
                }
            }
        }, 1);
    }

    private void disableUp(ClientApi api, Text reason) {
        enableUP = false;
        api.chat().printChatMessage(prefix.copy().append(Text.of("Быстрый апгрейд ", TextFormatting.GOLD, "выключен", TextFormatting.RED)).append(reason));
        if (taskUP != null) taskUP.shutdown();
        taskUP = null;
        api.threadManagement().newSingleThreadedScheduledExecutor().schedule(() -> {
            protectUp = false;
            api.chat().printChatMessage(prefix.copy().append(Text.of("Быстрый апгрейд ", TextFormatting.GOLD, "завершён", TextFormatting.YELLOW)));
        }, 5, TimeUnit.SECONDS);
    }

    private void disableBuy(ClientApi api, Text reason) {
        enableBuy = false;
        api.chat().printChatMessage(prefix.copy().append(Text.of("Быстрая покупка ", TextFormatting.GOLD, "выключена", TextFormatting.RED)).append(reason));
        if (taskBuy != null) taskBuy.shutdown();
        taskBuy = null;
        api.threadManagement().newSingleThreadedScheduledExecutor().schedule(() -> {
            protectBuy = false;
            api.chat().printChatMessage(prefix.copy().append(Text.of("Быстрая покупка ", TextFormatting.GOLD, "завершена", TextFormatting.YELLOW)));
        }, 5, TimeUnit.SECONDS);
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
            if (str.isEmpty()) continue;
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
}