package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.theinfinitymc.infinitybot.InfinityBot;

public class Play implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		if (args.length > 0) {
			for (String song : args) {
				InfinityBot.getAudio().addToQueue(song, event.getGuild(), event.getTextChannel(), event.getAuthor());
			}
		}
	}

	@Override
	public String getDescription() {
		return "Plays audio from the specified link. Must be in voice chat.";
	}

}
