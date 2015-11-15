# Плагин для Russian AI Cup 2015: CodeRacing

Повзаимствовал код [cjey](http://russianaicup.ru/forum/index.php?topic=400.0), "немного" изменил (т.к. пишу на C++) и дополнил кодом [EnjoyLife](https://github.com/AlexGeryavenko/ruaicup-2015-local-runner-pro).

### Связка состоит из трех частей:

1. **Плагин**. В нем наработки [EnjoyLife](http://russianaicup.ru/forum/index.php?topic=400.0) (разукраска чекпоинтов) + код [cjey](http://russianaicup.ru/forum/index.php?topic=392.msg3943#msg3943) (java-клиент).
2. **MyDebug.h**. Заголовок.
3. **MyDebug.cpp**. Сервер создается в другом потоке и ожидает клиента (из java-плагина).

Модернизировать код не планирую, но все желающие могут повстраивать этот код в свои публичные наработки. Я буду польщен :)

### Пример использования:

```c++
MyDebug debug;
void MyStrategy::move(const Car& me, const World& world, const Game& game, Move& move) {
    debug.lockFrame();
    debug.setColor(0x33aa77);
    debug.fillCircle(me.getX(), me.getX(), 1600 + world.getTick());
    debug.unlockFrame();
}
```

### Есть скрытые возможности.

Например, если вы собираетесь запустить несколько своих стратегий, то можно в раннере выставить флаг **MyDebugEnabled** в **false**. Это нужно чтобы другая стратегия не заняла порт обмена информацией с плагином.
