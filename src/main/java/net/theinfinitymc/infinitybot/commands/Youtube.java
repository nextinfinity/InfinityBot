package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Youtube implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		if(args.length >= 2){
			int i = 3;
			String m = args[2];
			while(i < args.length){
				m = m + " " + args[i];
				i++;
			}
			final String query = m;
			attemptAddToQueue(youtube.search(query), e.getGuild(), e.getTextChannel(), e.getAuthor());
		}
	}

	@Override
	public String getDescription() {
		return null;
	}
	
}
