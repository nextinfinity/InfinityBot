package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class Info implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		Message text = new MessageBuilder().append("__**Infinity Bot**__"
				+ "\n" + "Version: " + "2.1.2"
				+ "\n" + "API: JDA v" + "4.2.0_211" + " w/ LavaPlayer v" + "1.3.55"
				+ "\n" + "Creator: NextInfinity").build();
		event.getChannel().sendMessage(text).queue();
	}

	@Override
	public String getDescription() {
		return "Displays basic bot info.";
	}

}
