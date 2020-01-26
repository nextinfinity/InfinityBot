package net.theinfinitymc.infinitybot.commands.admin;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.theinfinitymc.infinitybot.commands.Command;
import net.theinfinitymc.infinitybot.utils.ArgBuilder;

import java.util.List;

public class UserList implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		if (args.length > 0) {
			String name = ArgBuilder.buildString(args);
			List<Guild> guilds = event.getJDA().getGuildsByName(name, true);
			if (guilds != null && guilds.size() >= 1) {
				Guild guild = guilds.get(0);
				StringBuilder text = new StringBuilder("__**Users in " + guild.getName() + "**__");
				for (Member mem : guild.getMembers()) {
					text.append("\n").append(mem.getEffectiveName());
				}
				event.getChannel().sendMessage(text.toString()).queue();
			}
		}
	}

	@Override
	public String getDescription() {
		return null;
	}

}
