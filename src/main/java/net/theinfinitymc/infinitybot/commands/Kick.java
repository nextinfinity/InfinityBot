package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class Kick implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		if (event.getMember().hasPermission(Permission.KICK_MEMBERS)) {
			if (args.length > 0) {
				for (User user : event.getMessage().getMentionedUsers()) {
					event.getGuild().kick(user.getId()).queue();
				}
			}
		}
	}

	@Override
	public String getDescription() {
		return "Kicks any mentioned user(s) from the server.";
	}

}
