package net.theinfinitymc.infinitybot.commands.admin;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.theinfinitymc.infinitybot.commands.Command;
import net.theinfinitymc.infinitybot.utils.ArgBuilder;

import java.util.List;

public class ServerInfo implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		String name = ArgBuilder.buildString(args);
		List<Guild> guilds = event.getJDA().getGuildsByName(name, true);
		if (guilds != null && guilds.size() >= 1) {
			Guild guild = guilds.get(0);
			String text = "__**Server Info: " + guild.getName() + "**__"
					+ "\nOwner: " + guild.getOwner().getEffectiveName()
					+ "\nUsers: " + guild.getMembers().size()
					+ "\nText Channels: " + guild.getTextChannels().size()
					+ "\nVoice Channels: " + guild.getVoiceChannels().size();
			event.getChannel().sendMessage(text).queue();
		}
	}

	@Override
	public String getDescription() {
		return null;
	}
}
