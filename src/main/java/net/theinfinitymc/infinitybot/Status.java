package net.theinfinitymc.infinitybot;

import net.dv8tion.jda.core.entities.Game;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

class Status {
    private final static Timer TIMER = new Timer();
    private final static Game[] GAMES = {
            Game.listening("Franky Nuts - All I Want"),
            Game.listening("Kayzo - Never Alone"),
            Game.listening("Periphery - Alpha"),
            Game.listening("Red Cold River"),
            Game.playing("HuniePop"),
            Game.watching("CornHub"),
            Game.watching("bot.nextinfinity.net"),
            Game.watching("bot.nextinfinity.net"),
            Game.watching("bot.nextinfinity.net"),
            Game.watching("Type .help for help!"),
            Game.playing("Counter-Strike: Global Offensive"),
            Game.playing("Apex Legends"),
            Game.listening("Periphery IV"),
            Game.listening("Mr. Bill - Apophenia"),
            Game.listening("YUNG MODEM")};

    private static int index;
    private static int interval = 2;

    static void start() {
        TIMER.purge();
        index = new Random().nextInt(GAMES.length);
        while (GAMES.length % interval != 0) {
            interval++;
        }
        TIMER.scheduleAtFixedRate(
                new TimerTask() {
                    @Override
                    public void run() {
                        index += interval;
                        if (index >= GAMES.length) index -= GAMES.length;
                        Game game = GAMES[index];
                        InfinityBot.getAPI().getPresence().setGame(game);
                    }
                }
                , 0, 5 * 60 * 1000);
    }
}
