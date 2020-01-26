package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.theinfinitymc.infinitybot.InfinityBot;

public class Pause implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		InfinityBot.getAudio().togglePause(event.getGuild(), event.getTextChannel());
	}

	@Override
	public String getDescription() {
		return "Pause/unpause the audio, if any is playing.";
	}

}
