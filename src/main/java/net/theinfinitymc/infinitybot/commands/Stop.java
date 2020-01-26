package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.theinfinitymc.infinitybot.InfinityBot;

public class Stop implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		InfinityBot.getAudio().stop(event.getGuild());
	}

	@Override
	public String getDescription() {
		return "Stops audio, if it is playing.";
	}

}
