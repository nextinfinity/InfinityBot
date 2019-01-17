package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageHistory;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.List;

public class Clear implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		if(e.getMember().hasPermission(Permission.MESSAGE_MANAGE)){
			delete(e.getMessage());
			int amount = 10;
			if(args.length > 1){
				int i = Integer.parseInt(args[1]);
				if(i > 0) amount = i;
			}
			List<Message> list = new MessageHistory(e.getChannel()).retrievePast(amount).complete();
			for(Message m : list){
				delete(m);
			}
		}
	}

	public void delete(Message m){
		try{
			m.delete().queue();
		}catch(Exception e){}
	}

	@Override
	public String getDescription() {
		return null;
	}

}
