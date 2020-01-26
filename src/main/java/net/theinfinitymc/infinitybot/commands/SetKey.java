package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.theinfinitymc.infinitybot.utils.Config;

public class SetKey implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		if (event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
			if (args.length == 1) {
				try {
					Config.setKey(args[0], event.getGuild());
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	@Override
	public String getDescription() {
		return "Sets the key for commands for the server. Default: .";
	}

}
