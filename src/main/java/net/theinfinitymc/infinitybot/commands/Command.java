package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public interface Command {

	void execute(MessageReceivedEvent event, String[] args);

	String getDescription();
}
