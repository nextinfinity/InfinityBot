package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Play implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		if(args.length == 2){
			String song = args[1];
			attemptAddToQueue(song, e.getGuild(), e.getTextChannel(), e.getAuthor());
		}
	}

	@Override
	public String getDescription() {
		return null;
	}

}
