package net.theinfinitymc.infinitybot.commands.admin;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.theinfinitymc.infinitybot.commands.Command;

public class Servers implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		String text = "__**Joined Servers**__";
		for (Guild g : event.getJDA().getGuilds()) {
			text = text + "\n" + g.getName();
		}
		event.getChannel().sendMessage(text).queue();
	}

	@Override
	public String getDescription() {
		return null;
	}

}
