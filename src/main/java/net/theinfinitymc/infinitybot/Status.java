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
			Game.watching("inmc.pw/bot | .help"),
			Game.watching("inmc.pw/bot | .help")};

	private static int i;
	private static int n = 2;

	static void start(){
		TIMER.purge();
		i = new Random().nextInt(GAMES.length);
		while(GAMES.length % n != 0){
			n++;
		}
		TIMER.scheduleAtFixedRate(
				new TimerTask(){
					@Override
					public void run(){
						i += n;
						if(i >= GAMES.length) i -= GAMES.length;
						Game game = GAMES[i];
						InfinityBot.getAPI().getPresence().setGame(game);
					}
				}
				,0,5*60*1000);
	}

}
