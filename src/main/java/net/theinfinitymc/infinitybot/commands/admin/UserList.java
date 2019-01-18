package net.theinfinitymc.infinitybot.commands.admin;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
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
				String text = "__**Users in " + guild.getName() + "**__";
				for (Member mem : guild.getMembers()) {
					text = text + "\n" + mem.getEffectiveName();
				}
				event.getChannel().sendMessage(text).queue();
			}
		}
	}

	@Override
	public String getDescription() {
		return null;
	}

}
