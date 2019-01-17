package net.theinfinitymc.infinitybot.commands;

import net.dean.jraw.models.Message;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Kick implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		if(event.getMember().hasPermission(Permission.KICK_MEMBERS)){
			if(args.length > 1){
				for(User user : event.getMessage().getMentionedUsers()){
					event.getGuild().getController().kick(user.getId()).queue();
				}
			}
		}
	}

	@Override
	public String getDescription() {
		return null;
	}

}