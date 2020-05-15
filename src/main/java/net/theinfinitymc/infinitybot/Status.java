package net.theinfinitymc.infinitybot;

import net.dv8tion.jda.api.entities.Activity;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

class Status {
    private final static Timer TIMER = new Timer();
    private final static Activity[] GAMES = {
            Activity.watching("bot.nextinfinity.net"),
            Activity.watching("Type .help for help!"),
            Activity.watching("discord.gg/PvmhyMs for support")
    };

    private static int index;

    static void start() {
        TIMER.purge();
        index = new Random().nextInt(GAMES.length);
        TIMER.scheduleAtFixedRate(
                new TimerTask() {
                    @Override
                    public void run() {
                        index++;
                        if (index >= GAMES.length) index -= GAMES.length;
                        Activity game = GAMES[index];
                        InfinityBot.getAPI().getPresence().setActivity(game);
                    }
                }
                , 0, 5 * 60 * 1000);
    }
}
