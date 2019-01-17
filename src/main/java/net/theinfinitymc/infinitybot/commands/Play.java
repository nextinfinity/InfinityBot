package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.theinfinitymc.infinitybot.InfinityBot;

public class Play implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		if(args.length == 2){
			String song = args[1];
			InfinityBot.getAudio().addToQueue(song, event.getGuild(), event.getTextChannel(), event.getAuthor());
		}
	}

	@Override
	public String getDescription() {
		return null;
	}

}
