package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.theinfinitymc.infinitybot.InfinityBot;
import net.theinfinitymc.infinitybot.utils.Config;

import java.io.File;

public class Noot implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		String file = Config.DIR + File.pathSeparator + "noot.mp3";
		InfinityBot.getAudio().addToQueue(file, event.getGuild(), event.getTextChannel(), event.getAuthor());
	}

	@Override
	public String getDescription() {
		return "NOOT NOOT";
	}

}
