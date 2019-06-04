package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.theinfinitymc.infinitybot.InfinityBot;

public class AnothaOne implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		String link = "https://soundcloud.com/vagidictoris/skrillex-gives-him-another-one";
		InfinityBot.getAudio().addToQueue(link, event.getGuild(), event.getTextChannel(), event.getAuthor());
	}

	@Override
	public String getDescription() {
		return "ANOTHA ONE";
	}

}
