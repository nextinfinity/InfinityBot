package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Info implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		Message text = new MessageBuilder().append("__**Infinity Bot**__"
				+ "\n" + "Version: " + "@botVersion@"
				+ "\n" + "API: JDA v" + "@jdaVersion@" + " w/ LavaPlayer v" + "@lavaVersion@"
				+ "\n" + "Creator: NextInfinity").build();
		event.getChannel().sendMessage(text).queue();
	}

	@Override
	public String getDescription() {
		return null;
	}

}
