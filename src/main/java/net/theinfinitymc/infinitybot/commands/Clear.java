package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;

public class Clear implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		if (event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {
			delete(event.getMessage());
			int amount = 10;
			if (args.length > 0) {
				int i = Integer.parseInt(args[0]);
				if (i > 0) amount = i;
			}
			List<Message> list = new MessageHistory(event.getChannel()).retrievePast(amount + 1).complete();
			for (Message m : list) {
				delete(m);
			}
		}
	}

	private void delete(Message m) {
		try {
			m.delete().queue();
		} catch (Exception e) {
			//Silently fail if the message can't be deleted, don't want to clog up chat more
		}
	}

	@Override
	public String getDescription() {
		return "Delete the specified number of messages.";
	}

}
