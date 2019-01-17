package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Ping implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		event.getChannel().sendMessage("Pong!").queue();
	}

	@Override
	public String getDescription() {
		return null;
	}
}
