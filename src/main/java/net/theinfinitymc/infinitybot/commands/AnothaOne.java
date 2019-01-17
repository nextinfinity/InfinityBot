package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class AnothaOne implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		attemptAddToQueue("https://soundcloud.com/vagidictoris/skrillex-gives-him-another-one", e.getGuild(), e.getTextChannel(), e.getAuthor());
	}

	@Override
	public String getDescription() {
		return null;
	}

}
