Сразу скажу, инжекторы не мои, один из них лишь мною модифицировался.

В bypass.jar содержатся классы которые модифицируют minecraft.jar или core-mode.jar, в частности там убирается проверка на @SidedApi(Side.SERVER) также проверки запрещающее доступ модам ко внутренним классам майнкрафта  
в ru.cristalix.client.CristalixClient.class идёт подгрузка модов из папки C:\Xenoceal\mods
в dev.xdark.clientapi.loader.ClientVerifier.class убираются проверки на доступ к внутренним классам майнкрафта   
dev.xdark.clientapi.loader.Verifier.class это интерфейс класса dev.xdark.clientapi.loader.ClientVerifier.class, иначе без него не запускается майнкрафт

sucec.jar не мой, скорее всего это Xenoceal'а, я просил его дать мне исходники но я получил отказ из-за чего мне пришлось его ковырять, там немного изменены приоритеты на подгрузку классов в bypass.jar

Исходного кода на .jar нет, правки делалались манипуляциями с байткодом