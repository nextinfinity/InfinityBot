package net.theinfinitymc.infinitybot.modules;

import net.dv8tion.jda.core.entities.Game;
import net.theinfinitymc.infinitybot.InfinityBot;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class Status {
    private static Timer timer = new Timer();
    private static Game[] games = {
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

    private static int i;
    private static int n = 2;

    public static void start(){
        timer.purge();
        i = new Random().nextInt(games.length);
        while(true){
            if(games.length % n != 0) break;
            n++;
        }
        timer.scheduleAtFixedRate(
                new TimerTask(){
                    @Override
                    public void run(){
                        i = i+n;
                        if(i >= games.length) i = i-games.length;
                        Game game = games[i];
                        InfinityBot.api.getPresence().setGame(game);
                    }
                }
        ,0,5*60*1000);
    }
}
