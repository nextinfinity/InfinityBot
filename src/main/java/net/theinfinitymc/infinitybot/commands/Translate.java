package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.theinfinitymc.infinitybot.InfinityBot;

public class Translate implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		if(args.length >= 3){
			int i = 3;
			String m = args[2];
			while(i < args.length){
				m = m + " " + args[i];
				i++;
			}
			final String query = m;
			final String lang = args[1].toLowerCase();
			InfinityBot.getThreadPool().execute(new Runnable(){
				@Override
				public void run(){
					try {
						e.getChannel().sendMessage(translate.translate(query, lang)).queue();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
	}

	@Override
	public String getDescription() {
		return null;
	}

}
