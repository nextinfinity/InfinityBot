package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.theinfinitymc.infinitybot.utils.Config;

public class SetKey implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		if(event.getMember().hasPermission(Permission.ADMINISTRATOR)){
			if(args.length == 2){
				try{
					Config.setKey(args[1], event.getGuild());
				}catch(Exception ex){
					ex.printStackTrace();
				}
			}
		}
	}

	@Override
	public String getDescription() {
		return null;
	}

}
