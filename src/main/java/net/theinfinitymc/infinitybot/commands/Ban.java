package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class Ban implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		if (event.getMember().hasPermission(Permission.BAN_MEMBERS)) {
			if (args.length > 0) {
				for (User user : event.getMessage().getMentionedUsers()) {
					event.getGuild().ban(user.getId(), 0).queue();
				}
			}
		}
	}

	@Override
	public String getDescription() {
		return "Bans any mentioned user(s) from the server.";
	}

}
