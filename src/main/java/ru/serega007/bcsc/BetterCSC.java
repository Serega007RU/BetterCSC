package ru.serega007.bcsc;

import com.xenoceal.cristalix.Reflection;
import com.xenoceal.cristalix.Wrapper;
import dev.xdark.clientapi.entity.EntityPlayerSP;
import dev.xdark.clientapi.event.chat.ChatReceive;
import dev.xdark.clientapi.event.input.MousePress;
import dev.xdark.clientapi.event.network.PluginMessage;
import dev.xdark.clientapi.event.network.ServerSwitch;
import dev.xdark.clientapi.event.render.*;
import dev.xdark.clientapi.event.chat.ChatSend;
import dev.xdark.clientapi.item.ItemStack;
import dev.xdark.clientapi.network.NetworkPlayerInfo;
import dev.xdark.clientapi.text.Text;
import dev.xdark.clientapi.text.TextFormatting;
import dev.xdark.clientapi.ClientApi;
import dev.xdark.clientapi.event.Listener;
import dev.xdark.clientapi.entry.ModMain;
import dev.xdark.clientapi.util.EnumHand;
import dev.xdark.clientapi.world.GameMode;
import dev.xdark.feder.NetUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class BetterCSC implements ModMain, Listener {
    private boolean hp = true;

    //Скрытие флуда в чате при быстрой прокачке/покупке
    private Text lastMessage = null;
    private long lastMessageTimeMillis;

    //Подсчёт общей суммы ставок
    private long allBets = 0;
    private boolean countBets = false;
    private boolean allowedBuy = true;

    //Быстрая прокачка меча без лагов
    private boolean enableUP = false;
    private boolean pausedUp = false;
    private boolean protectUp = false;
    private ScheduledExecutorService taskUP = null;
    private ScheduledExecutorService taskProtectUP = null;
    private int countUp = 0;

    //Быстрая покупка книг с авто-использованием
    private boolean enableBuy = false;
    private boolean pausedBuy = false;
    private ScheduledExecutorService taskBuy = null;
    private ScheduledExecutorService taskUse = null;
    private int countBuy = 0;
    private int countUse = 0;
    private String uuidWindowBuy = null;
    private int slotForBuy = 0;

    //Авто-ставки
//    private boolean doAutoBet = false;

    //Оповещение в чате когда кто-то заходит/выходит в катке CSC
    private Collection<NetworkPlayerInfo> playerList;
    private boolean startGame = false;
    private long balance = 0;
    private final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)" + '§' + "[0-9A-FK-OR]");


    //Префикс мода
    private final Text prefix = Text.of("[", TextFormatting.DARK_RED, "BetterCSC", TextFormatting.DARK_PURPLE, "]", TextFormatting.DARK_RED, " ");

    @Override
    public void load(ClientApi api) {
        try {
            Reflection.initialize();
        } catch (Exception e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            api.chat().printChatMessage(prefix.copy().append(Text.of("Ошибка загрузки маппингов, обратитесь к разработчику https://gitlab.com/Serega007/bettercsc/-/issues " + e.getLocalizedMessage(), TextFormatting.DARK_RED)));
            return;
        }

        api.chat().printChatMessage(prefix.copy().append(Text.of("Plus Edition", TextFormatting.DARK_AQUA, " версии ", TextFormatting.GOLD, "2.6.14", TextFormatting.YELLOW, " загружен, by ", TextFormatting.GOLD, "Serega007", TextFormatting.DARK_GREEN, " & ", TextFormatting.GOLD, "VVHIX", TextFormatting.DARK_GREEN)));
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

                    int slot;
                    int count;
                    int period;
                    try {
                        String[] args = msg.split(" ");
                        slot = Integer.parseInt(args[1]);
                        count = Integer.parseInt(args[2]);
                        period = Integer.parseInt(args[3]);
                        if (slot < 1 || slot > 9) {
                            api.chat().printChatMessage(prefix.copy().append(Text.of("Неверно указан номер слота, он может быть только от 1 до 9", TextFormatting.RED)));
                            return;
                        }
                        if (count > 32766) {
                            api.chat().printChatMessage(prefix.copy().append(Text.of("Куда так много? Максимум можно 32766 кол-во", TextFormatting.RED)));
                            return;
                        }
                        if (period > 500) {
                            api.chat().printChatMessage(prefix.copy().append(Text.of("Куда так много? Максимум можно 500 период", TextFormatting.RED)));
                            return;
                        }
                        if (count <= 0 || period <= 0) throw new RuntimeException();
                    } catch (Exception e) {
                        api.chat().printChatMessage(this.prefix.copy().append(Text.of("Неверно указаны числа или аргументы, ", TextFormatting.RED, "/up ", TextFormatting.AQUA, "<", TextFormatting.GRAY, "номер слота", TextFormatting.LIGHT_PURPLE, "> <", TextFormatting.GRAY, "кол-во", TextFormatting.LIGHT_PURPLE, "> <", TextFormatting.GRAY, "период", TextFormatting.LIGHT_PURPLE, ">", TextFormatting.GRAY)));
                        return;
                    }
                    EntityPlayerSP player = api.minecraft().getPlayer();
                    int id;
                    try {
                        id = player.getInventory().getStackInSlot(slot - 1).getItem().getId();
                    } catch (Exception e) {
                        api.chat().printChatMessage(prefix.copy().append(Text.of("В слоте ", TextFormatting.RED, String.valueOf(slot), TextFormatting.GOLD, " отсутствует предмет", TextFormatting.RED)));
                        return;
                    }
                    api.chat().printChatMessage(prefix.copy().append(Text.of("Быстрый апгрейд ", TextFormatting.GOLD, "включён", TextFormatting.GREEN, ", нажмите ", TextFormatting.GOLD, "СКМ", TextFormatting.RED, " что бы выключить", TextFormatting.GOLD)));

                    if (taskProtectUP != null) taskProtectUP.shutdownNow();
                    taskProtectUP = null;

                    enableUP = true;
                    pausedUp = false;
                    protectUp = true;
                    countUp = 0;

                    taskUP = api.threadManagement().newSingleThreadedScheduledExecutor();
                    taskUP.scheduleAtFixedRate(() -> {
                        if (!enableUP) return;
                        if (!allowedBuy) {
                            if (!pausedUp) {
                                pausedUp = true;
                                api.chat().printChatMessage(prefix.copy().append(Text.of("Быстрый апгрейд ", TextFormatting.GOLD, "приостановлен", TextFormatting.YELLOW, " так как началась волна/дуэль", TextFormatting.GOLD)));
                            }
                            return;
                        }
                        if (pausedUp) {
                            pausedUp = false;
                            api.chat().printChatMessage(prefix.copy().append(Text.of("Быстрый апгрейд ", TextFormatting.GOLD, "возобновлён", TextFormatting.GREEN)));
                        }
                        countUp++;
                        if (countUp > count) {
                            disableUp(api, Text.of(TextFormatting.RED, ", ", TextFormatting.GOLD, "достигли заданного числа", TextFormatting.GREEN));
                            return;
                        }
                        ByteBuf buffer;
                        ByteBuf $this$writeVarInt$iv = buffer = Unpooled.buffer();
                        NetUtil.writeVarInt(slot - 1, $this$writeVarInt$iv);
                        NetUtil.writeVarInt(id, buffer);
                        api.clientConnection().sendPayload("csc:upgrade", buffer);
                    }, 0, 1000000 / period, TimeUnit.MICROSECONDS);
                } else if (msg.startsWith("/buy")) {
                    chatSend.setCancelled(true);
                    if (enableBuy) {
                        disableBuy(api, Text.of(""));
                        return;
                    }
                    int id;
                    int count;
                    int periodBuy;
                    int periodUse;
                    try {
                        String[] args = msg.split(" ");
                        id = Integer.parseInt(args[1]);
                        count = Integer.parseInt(args[2]);
                        periodBuy = Integer.parseInt(args[3]);
                        periodUse = Integer.parseInt(args[4]);
                        if (count > 50000) {
                            api.chat().printChatMessage(prefix.copy().append(Text.of("Куда так много? Максимум можно 50000 кол-во", TextFormatting.RED)));
                            return;
                        }
                        if (periodBuy > 500) {
                            api.chat().printChatMessage(prefix.copy().append(Text.of("Куда так много? Максимум можно 500 период покупки", TextFormatting.RED)));
                            return;
                        }
                        if (periodUse > 500) {
                            api.chat().printChatMessage(prefix.copy().append(Text.of("Куда так много? Максимум можно 500 период использования", TextFormatting.RED)));
                            return;
                        }
                        if (id <= 0 || count <= 0 || periodBuy <= 0 || periodUse <= 0) throw new RuntimeException();
                    } catch (Exception e) {
                        api.chat().printChatMessage(this.prefix.copy().append(Text.of("Неверно указаны числа или аргументы, ", TextFormatting.RED, "/buy ", TextFormatting.AQUA, "<", TextFormatting.GRAY, "номер слота", TextFormatting.LIGHT_PURPLE, "> <", TextFormatting.GRAY, "кол-во", TextFormatting.LIGHT_PURPLE, "> <", TextFormatting.GRAY, "период закупки", TextFormatting.LIGHT_PURPLE, "> <", TextFormatting.GRAY, "период использования", TextFormatting.LIGHT_PURPLE, ">", TextFormatting.GRAY)));
                        return;
                    }

                    if (uuidWindowBuy == null) {
                        api.chat().printChatMessage(prefix.copy().append(Text.of("Похоже вы не открывали меню Магазина, откройте его (хотя бы на секунду) что б мод запомнил ID меню")));
                        return;
                    }

                    EntityPlayerSP player = api.minecraft().getPlayer();

                    slotForBuy = player.getInventory().getActiveSlot();

                    api.chat().printChatMessage(prefix.copy().append(Text.of("Быстрая покупка ", TextFormatting.GOLD, "включена", TextFormatting.GREEN, TextFormatting.GREEN, ", нажмите ", TextFormatting.GOLD, "СКМ", TextFormatting.RED, " что бы выключить, Выбран слот ", TextFormatting.GOLD, String.valueOf(slotForBuy + 1), TextFormatting.BLUE, " в качестве использования книг", TextFormatting.GOLD)));

                    enableBuy = true;
                    pausedBuy = false;
                    countBuy = 0;
                    countUse = 0;

                    taskBuy = api.threadManagement().newSingleThreadedScheduledExecutor();
                    taskBuy.scheduleAtFixedRate(() -> {
                        if (!enableBuy || uuidWindowBuy == null) return;
                        if (!allowedBuy) {
                            if (!pausedBuy) {
                                pausedBuy = true;
                                api.chat().printChatMessage(prefix.copy().append(Text.of("Быстрая покупка ", TextFormatting.GOLD, "приостановлена", TextFormatting.YELLOW, " так как началась волна/дуэль", TextFormatting.GOLD)));
                            }
                            return;
                        }
                        if (player.getInventory().getActiveSlot() != slotForBuy) {
                            if (!pausedBuy) {
                                pausedBuy = true;
                                api.chat().printChatMessage(prefix.copy().append(Text.of("Быстрая покупка ", TextFormatting.GOLD, "приостановлена", TextFormatting.YELLOW, " так как вы не держите слот ", TextFormatting.GOLD, String.valueOf(slotForBuy + 1), TextFormatting.BLUE, " активным", TextFormatting.GOLD)));
                            }
                            return;
                        }
                        if (pausedBuy) {
                            pausedBuy = false;
                            api.chat().printChatMessage(prefix.copy().append(Text.of("Быстрая покупка ", TextFormatting.GOLD, "возобновлена", TextFormatting.GREEN)));
                        }
                        // TODO мы не можем использовать книги в гм3
                        if (api.minecraft().getPlayerController().getGameMode().equals(GameMode.SPECTATOR)) return;
                        ItemStack itemStack = player.getInventory().getCurrentItem();
                        if (!itemStack.isEmpty() && (!itemStack.getDisplayName().contains("Книга") || itemStack.getCount() > 32)) return;
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
                    }, 0, 1000000 / periodBuy, TimeUnit.MICROSECONDS);

                    taskUse = api.threadManagement().newSingleThreadedScheduledExecutor();
                    taskUse.scheduleAtFixedRate(() -> {
                        if (!enableBuy || uuidWindowBuy == null) return;
                        if (!allowedBuy) return;
                        if (player.getInventory().getActiveSlot() != slotForBuy) return;
                        // TODO мы не можем использовать книги в гм3
                        if (api.minecraft().getPlayerController().getGameMode().equals(GameMode.SPECTATOR)) return;
                        ItemStack itemStack = player.getInventory().getCurrentItem();
                        if (!itemStack.isEmpty() && !itemStack.getDisplayName().contains("Книга")) return;
                        countUse++;
                        if (countUse > count) {
                            disableBuy(api, Text.of(TextFormatting.RED, ", ", TextFormatting.GOLD, "достигли заданного числа", TextFormatting.GREEN));
                            return;
                        }
                        try {
                            Wrapper.sendPacket(Wrapper.CPacketPlayerTryUseItem(EnumHand.MAIN_HAND));
                        } catch (Exception e) {
                            //noinspection CallToPrintStackTrace
                            e.printStackTrace();
                            disableBuy(api, Text.of("Произошла ошибка при попытке кликнуть ПКМ", TextFormatting.RED, ", ", TextFormatting.GOLD, e.toString(), TextFormatting.DARK_RED));
                        }
                    }, 0, 1000000 / periodUse, TimeUnit.MICROSECONDS);
                } else if (msg.startsWith("/unloadbcsc")) {
                    chatSend.setCancelled(true);
                    api.chat().printChatMessage(prefix.copy().append(Text.of("Выгружаем данный мод, пока =(", TextFormatting.WHITE)));
                    unload();
                } else if (msg.startsWith("/bet")) {
                    chatSend.setCancelled(true);
                    long sum;
                    int team;
                    try {
                        String[] args = msg.split(" ");
                        team = Integer.parseInt(args[1]);
                        if (args[2].contains("all")) {
                            sum = balance;
                        } else {
                            sum = Long.parseLong(args[2]);
                        }
                        if (team <= 0) {
                            throw new RuntimeException();
                        }
                    } catch (Exception var14) {
                        api.chat().printChatMessage(this.prefix.copy().append(Text.of("Неверно указаны числа или аргументы, ", TextFormatting.RED, "/bet ", TextFormatting.AQUA, "<", TextFormatting.GRAY, "номер команды", TextFormatting.LIGHT_PURPLE, "> <", TextFormatting.GRAY, "сумма ставки", TextFormatting.LIGHT_PURPLE, "/", TextFormatting.GRAY, "all", TextFormatting.DARK_PURPLE, ">", TextFormatting.GRAY)));
                        return;
                    }
                    if (sum == 0) {
                        api.chat().printChatMessage(this.prefix.copy().append(Text.of("Ставка 0? Ноль ты без палочки!", TextFormatting.RED)));
                        return;
                    }
                    if (team > 2) {
                        api.chat().printChatMessage(this.prefix.copy().append(Text.of("Неверно указан номер команды (тимы), ", TextFormatting.RED, "/bet ", TextFormatting.AQUA, "<", TextFormatting.GRAY, "номер команды", TextFormatting.LIGHT_PURPLE, "> <", TextFormatting.GRAY, "сумма ставки", TextFormatting.LIGHT_PURPLE, "/", TextFormatting.GRAY, "all", TextFormatting.DARK_PURPLE, ">", TextFormatting.GRAY)));
                        return;
                    }
                    long finalSum = sum;
                    if (sum > balance && balance != 0) {
                        finalSum = balance;
                    }
                    ByteBuf buffer;
                    ByteBuf $this$lambda_u246_u24lambda_u245 = buffer = Unpooled.buffer();
                    $this$lambda_u246_u24lambda_u245.writeInt(team - 1);
                    $this$lambda_u246_u24lambda_u245.writeLong(finalSum);
                    api.clientConnection().sendPayload("csc:make_bet", buffer);
                    if (balance == 0) {
                        api.chat().printChatMessage(this.prefix.copy().append(Text.of("Ставка (", TextFormatting.GREEN, new DecimalFormat("#,###").format(finalSum), TextFormatting.GOLD, ") поставлена. ", TextFormatting.GREEN, "Предупреждение, ваш баланс ",TextFormatting.RED, "ноль", TextFormatting.GOLD, ", ставка может быть не успешной", TextFormatting.RED)));
                    } else if (sum > balance) {
                        api.chat().printChatMessage(this.prefix.copy().append(Text.of("Ставка (", TextFormatting.GREEN, new DecimalFormat("#,###").format(finalSum), TextFormatting.GOLD, ") поставлена. ", TextFormatting.GREEN, "Предупреждение, ваш баланс (",TextFormatting.RED, new DecimalFormat("#,###").format(balance), TextFormatting.GOLD, ") меньше указанной суммы (", TextFormatting.RED, new DecimalFormat("#,###").format(sum), TextFormatting.GOLD, ") ставка может быть не успешной", TextFormatting.RED)));
                    } else {
                        api.chat().printChatMessage(this.prefix.copy().append(Text.of("Ставка (", TextFormatting.GREEN, new DecimalFormat("#,###").format(finalSum), TextFormatting.GOLD, ") поставлена успешно", TextFormatting.GREEN)));
                    }
                }
            }
        }, 100);

        ChatReceive.BUS.register(this, chatReceive -> {
            String msg = chatReceive.getText().getUnformattedText();
            String msgColored = chatReceive.getText().getFormattedText().toLowerCase();

            if (this.hp) {

                if (msgColored.contains("§cу вас максимальное количество денег")) {
                    chatReceive.setCancelled(true);
                    return;
                }

                if (enableUP || enableBuy || protectUp) {
                    if (msgColored.contains("§aбаланс: ") || msgColored.contains("§aвы успешно улучшили предмет") || msgColored.contains("§aвы успешно купили предмет")) {
                        chatReceive.setCancelled(true);
                        return;
                    } else if (msgColored.contains("§cу вас недостаточно золота на балансе") || msgColored.contains("§cвы уже купили этот предмет") || msgColored.contains("§cэтот предмет нельзя улучшить") /*|| msgColored.contains("§cвы не находитесь в игре")*/ || msgColored.contains("§cошибка, вы не можете сейчас открыть меню апгрейда") || msgColored.contains("§cэтот предмет улучшен до максимального уровня") || msgColored.contains("§cу вас недостаточно места в инвентаре") || msgColored.contains("§cвы не можете сейчас покупать предметы")) {
                        chatReceive.setCancelled(true);
                        if (msgColored.contains("§cвы не можете сейчас покупать предметы") || msgColored.contains("§cошибка, вы не можете сейчас открыть меню апгрейда")) {
                            allowedBuy = false;
                            return;
                        }
                        if (enableUP) {
                            disableUp(api, Text.of(TextFormatting.RED, ", " + TextFormatting.GOLD, msg, TextFormatting.RED));
                        } else if (enableBuy) {
                            disableBuy(api, Text.of(TextFormatting.RED, ", " + TextFormatting.GOLD, msg, TextFormatting.RED));
                        }
                        return;
                    }
                }

                if (msgColored.contains("§aбаланс: ")) {
                    chatReceive.setCancelled(true);
                    if (lastMessage != null) {
                        api.chat().printChatMessage(lastMessage);
                    }
                    String text = chatReceive.getText().getFormattedText();
                    // TODO getUnformattedText должен возвращать текст без знаков цвета но почему-то стало ровно по другому
                    // TODO что-то кристаликс сломал в этой функции по этому приходится самому коды цветов выковыривать
                    String[] list = STRIP_COLOR_PATTERN.matcher(msg).replaceAll("").replaceAll("[^-?0-9]+", " ").trim().split(" ");
                    for (String str : list) {
                        long num = Long.parseLong(str);
                        text = text.replaceAll(str, new DecimalFormat("#,###").format(num));
                    }
                    chatReceive.setText(stringToText(text));
                    lastMessage = chatReceive.getText();
                    lastMessageTimeMillis = System.currentTimeMillis();
                } else if (msgColored.contains("§aвы успешно улучшили предмет") || msgColored.contains("§aвы успешно купили предмет")) {
                    chatReceive.setCancelled(true);
                    lastMessage = null;
                } else if (msgColored.contains("§aставки выиграли:")) {
                    countBets = true;
                } else if (countBets) {
                    if (msgColored.contains("§7- ")) {
                        String text = msg;
                        text = msg.substring(text.indexOf(" - ") + 3, text.length());
                        long num = Long.parseLong(text);
                        allBets += num;

                        String unfText = chatReceive.getText().getFormattedText();
                        unfText = unfText.replaceAll(String.valueOf(num), new DecimalFormat("#,###").format(num));
//                        chatReceive.setText(stringToText(unfText));
                        chatReceive.setCancelled(true);
                        api.chat().printChatMessage(stringToText(unfText));
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
//                    chatReceive.setText(stringToText(text));
                    chatReceive.setCancelled(true);
                    api.chat().printChatMessage(stringToText(text));
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

        ServerSwitch.BUS.register(this, serverSwitch -> {
            uuidWindowBuy = null;
            balance = 0;
            disableUp(api, Text.of(TextFormatting.RED, ", " + TextFormatting.GOLD, "Вы вышли из игры", TextFormatting.RED));
            disableBuy(api, Text.of(TextFormatting.RED, ", " + TextFormatting.GOLD, "Вы вышли из игры", TextFormatting.RED));
            allowedBuy = true;
        }, 1);

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
                    //Да тут просто триллион костылей, что бы на этом говно API Cristalix хоть как-то работало
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
                            String nick = player1.getDisplayName().getFormattedText().substring(player1.getDisplayName().getFormattedText().indexOf("§r§f") + 6);
                            if (nick.startsWith("§r§f ")) nick = nick.substring(6);
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
                            String nick = player1.getDisplayName().getFormattedText().substring(player1.getDisplayName().getFormattedText().indexOf("§r§f") + 6);
                            if (nick.startsWith("§r§f ")) nick = nick.substring(6);
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
            } else if (pluginMessage.getChannel().equals("csc:balance")) {
                balance = NetUtil.readVarLong(pluginMessage.getData());
                if (balance <= 0) { // TODO иногда при баге бесконечного баланса мы получаем нулевой баланс
                    balance = Integer.MAX_VALUE;
                }
            // TODO со скорборда баланс приходит с задержкой
//            } else if (pluginMessage.getChannel().equals("func:scoreboard-update")) {
//                ByteBuf byteBuf = pluginMessage.getData().copy();
//                NetUtil.readId(byteBuf);
//                if (NetUtil.readUtf8(byteBuf).equals("i18n.csc.game.gold")) {
//                    balance = Long.parseLong(STRIP_COLOR_PATTERN.matcher(NetUtil.readUtf8(byteBuf)).replaceAll(""));
//                }
            } else if (pluginMessage.getChannel().equals("func:notice")) {
                String message = NetUtil.readUtf8(pluginMessage.getData().copy());
                if (message.contains("wave.started")) {
                    allowedBuy = false;
                } else if (message.contains("wave.complete")) {
                    allowedBuy = true;
                    if (enableBuy) changeActiveSlot(api, slotForBuy);
                }
            } else if (pluginMessage.getChannel().equals("func:drop-item")) {
                allowedBuy = true;
                if (enableBuy) changeActiveSlot(api, slotForBuy);
            } else if (pluginMessage.getChannel().equals("func:title")) {
                if (enableUP || enableBuy) {
                    if (NetUtil.readUtf8(pluginMessage.getData().copy()).equals("i18n.csc.game.not.gold")) {
                        if (enableUP) {
                            disableUp(api, Text.of(TextFormatting.RED, ", " + TextFormatting.GOLD, "У вас недостаточно золота", TextFormatting.RED));
                        } else if (enableBuy) {
                            disableBuy(api, Text.of(TextFormatting.RED, ", " + TextFormatting.GOLD, "У вас недостаточно золота", TextFormatting.RED));
                        }
                    }
                }
            }
        }, 100);

        MousePress.BUS.register(this, mousePress -> {
            if (mousePress.getButton() == 2 && mousePress.getState()) {
                if (enableUP) {
                    disableUp(api, Text.of(""));
                }
                if (enableBuy) {
                    disableBuy(api, Text.of(""));
                }
            }
        }, 1);
    }

    private void disableUp(ClientApi api, Text reason) {
        if (!enableUP) return;
        enableUP = false;
        pausedUp = false;
        api.chat().printChatMessage(prefix.copy().append(Text.of("Быстрый апгрейд ", TextFormatting.GOLD, "выключен", TextFormatting.RED)).append(reason));
        if (taskUP != null) taskUP.shutdownNow();
        taskUP = null;

        if (taskProtectUP != null) taskProtectUP.shutdownNow();
        taskProtectUP = null;
        taskProtectUP = api.threadManagement().newSingleThreadedScheduledExecutor();
        taskProtectUP.schedule(() -> {
            if (enableUP) return;
            protectUp = false;
            taskProtectUP = null;
            api.chat().printChatMessage(prefix.copy().append(Text.of("Быстрый апгрейд ", TextFormatting.GOLD, "завершён", TextFormatting.YELLOW)));
        }, 15, TimeUnit.SECONDS);
    }

    private void disableBuy(ClientApi api, Text reason) {
        if (!enableBuy) return;
        enableBuy = false;
        pausedBuy = false;
        api.chat().printChatMessage(prefix.copy().append(Text.of("Быстрая покупка ", TextFormatting.GOLD, "выключена", TextFormatting.RED)).append(reason));
        if (taskBuy != null) taskBuy.shutdownNow();
        taskBuy = null;
        if (taskUse != null) taskUse.shutdownNow();
        taskUse = null;
    }

    private void changeActiveSlot(ClientApi api, int slot) {
        try {
            Wrapper.changeActiveSlot(api.minecraft().getPlayer().getInventory(), slot);
        } catch (Exception e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            disableBuy(api, Text.of("Произошла ошибка при попытке сменить активный слот", TextFormatting.RED, ", ", TextFormatting.GOLD, e.toString(), TextFormatting.DARK_RED));
        }
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