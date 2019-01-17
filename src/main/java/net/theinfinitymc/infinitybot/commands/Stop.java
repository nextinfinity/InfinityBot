package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Stop implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		audio.stop(e.getGuild());
	}

	@Override
	public String getDescription() {
		return null;
	}

}
