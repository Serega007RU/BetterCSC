# Мод BetterCSC для Cristalix для мини игры Custom Steve Chaos
    
## Как установить:
1. [Скачать](https://gitlab.com/Serega007/bettercsc/-/raw/plus/build/BetterCSC-Plus.jar) мод
2. Закинуть скачанный файл в `C:\Users\ИмяПользователя\.cristalix\updates\Minigames\mods`
3. Перезапустить игру если она была запущена в этот момент

Plus Edition работает только на взломанном лаунчере Cristalix'а с модифицированным core-mod   
IP Cristalix'a: `213.32.26.43`
    
## Что добавляет этот мод:
- Вместо сердечек (хп) отображается полоска хп с точным кол-во хп и процентами (это сделано что бы на пуш волнах хп не мешали игре)
- Все числа что пишутся в чате (деньги (монеты)) разделяются запятой для удобства определения тысячи это или милионы
- На ставках на дуэлях в чате пишется общая сумма ставок
- В чате оповещается когда кто-то входит или выходить с катки, можно так палить спектаторов (в особенности модеров и хелперов) (работает иногда с задержкой или вовсе не работает)
![](https://i.imgur.com/lq9FHWi.png)
- Топ рейтинга больше не пропадает а остаётся на всю игру в лобби (работает криво, таблица может накладываться друг на друга)
![](https://i.imgur.com/tTzkKuy.png)
- Возвращает стандартный таб (список игроков), что бы 2 таба друг на друга не накладывались, нужно в настройках управления перебиндить клавишу таба на другую
![](https://i.imgur.com/U1m7u70.png)
- Отключены сообщения о успешной прокачки предмета или покупки книги дабы избежать флуда в чате для пуш волн
- Команда `/leadertop` отображает топ рейтинга в текущей катке
- Команда `/hp` вкл/выкл мода
- Команда `/unloadbcsc` отключает мод без возможности включить его обратно

## Что добавляет Plus Edition:
- Команда `/upgrade` открыть почти где угодно меню прокачки
- `/up Число` быстрая прокачка предмета без лагов (скорость 500 в секунду)
- Alt + СКМ по нужному слоту - быстрая покупка книг и автоматическое его использование (скорость 20-25 книг в секунду)
- `/period up Число` настроить насколько быстро в секунду прокачивать предмет (по умолчанию 500 в секунду)
- `/period buy Число` настроить насколько быстро в секунду покупать и юзать книги (по умолчанию 25 в секунду)
- `/forcesingleup` переключить режим покупки книг для забагованной менюшки покупки книг с фиксированной ценой (по умолчанию включён)
- `/mod load НазваниеМода` `/mod unload НазваниеМода` позволяет временно отключить серверные моды кристаликса (в частности это нужно что бы отключить говно-мод из-за которого лагает когда прокачиваешь быстро предмет), `/mod list` что бы узнать список модов
- `/sendpayload Название` отправить пакет кристаликсу по PayLoad (например `/sendpayload csc:specmenu`)
