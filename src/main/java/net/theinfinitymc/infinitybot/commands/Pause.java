package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Pause implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		audio.togglePause(e.getGuild(), e.getTextChannel());
	}

	@Override
	public String getDescription() {
		return null;
	}

}
