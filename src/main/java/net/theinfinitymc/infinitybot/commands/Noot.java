package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Noot implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		attemptAddToQueue("noot.mp3", e.getGuild(), e.getTextChannel(), e.getAuthor());
	}

	@Override
	public String getDescription() {
		return null;
	}

}
