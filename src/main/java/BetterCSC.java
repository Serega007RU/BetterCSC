import com.google.gson.Gson;
import dev.xdark.clientapi.entity.EntityPlayerSP;
import dev.xdark.clientapi.event.chat.ChatReceive;
import dev.xdark.clientapi.event.network.PluginMessage;
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
import leaderboards.BoardContent;
import leaderboards.BoardStructure;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public class BetterCSC implements ModMain, Listener {
    private boolean hp = true;

    //Скрытие флуда в чате при быстрой прокачке/покупке
    private Text lastMessage = null;
    private long lastMessageTimeMillis;

    //Подсчёт общей суммы ставок
    private long allBets = 0;
    private boolean countBets = false;

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
        api.chat().printChatMessage(prefix.copy().append(Text.of("загружен, by ", TextFormatting.GOLD, "Serega007", TextFormatting.DARK_GREEN, " & ", TextFormatting.GOLD, "VVHIX", TextFormatting.DARK_GREEN)));
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
            if (this.hp) {
                String msg = chatReceive.getText().getUnformattedText();
                String msgColored = chatReceive.getText().getFormattedText();

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

        PluginMessage.BUS.register(this, pluginMessage -> {
            System.out.println("Channel:" + pluginMessage.getChannel());
            System.out.println("Datacha: " + NetUtil.readUtf8(pluginMessage.getData().copy()));
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
        HungerRender.BUS.unregisterAll(this);
        HealthRender.BUS.unregisterAll(this);
        ChatSend.BUS.unregisterAll(this);
        ChatReceive.BUS.unregisterAll(this);
        ArmorRender.BUS.unregisterAll(this);
        PlayerListRender.BUS.unregisterAll(this);
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
}