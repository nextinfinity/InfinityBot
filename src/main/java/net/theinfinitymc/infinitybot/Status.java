package net.theinfinitymc.infinitybot;

import net.dv8tion.jda.api.entities.Activity;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

class Status {
    private final static Timer TIMER = new Timer();
    private final static Activity[] GAMES = {
            Activity.listening("Franky Nuts - All I Want"),
            Activity.listening("Kayzo - Never Alone"),
            Activity.listening("Periphery - Alpha"),
            Activity.listening("Red Cold River"),
            Activity.playing("HuniePop"),
            Activity.watching("CornHub"),
            Activity.watching("bot.nextinfinity.net"),
            Activity.watching("bot.nextinfinity.net"),
            Activity.watching("bot.nextinfinity.net"),
            Activity.watching("Type .help for help!"),
            Activity.playing("Counter-Strike: Global Offensive"),
            Activity.playing("Apex Legends"),
            Activity.listening("Periphery IV"),
            Activity.listening("Mr. Bill - Apophenia"),
            Activity.listening("YUNG MODEM")};

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
                        Activity game = GAMES[index];
                        InfinityBot.getAPI().getPresence().setActivity(game);
                    }
                }
                , 0, 5 * 60 * 1000);
    }
}
