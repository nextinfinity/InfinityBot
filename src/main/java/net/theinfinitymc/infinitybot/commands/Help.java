package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.theinfinitymc.infinitybot.Listener;

import java.util.Map;

public class Help implements Command {

	private final Listener listener;

	public Help(Listener listener) {
		this.listener = listener;
	}

	//TODO Sort commands?
	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		Map<String, Command> commands = listener.getCommands();
		StringBuilder builder = new StringBuilder();
		for (String command : commands.keySet()) {
			String description = commands.get(command).getDescription();
			if (description != null) {
				builder.append(command);
				builder.append(": ");
				builder.append(description);
				builder.append("\n");
			}
		}
		String helpText = builder.toString();
		if (!helpText.equals("")) {
			event.getChannel().sendMessage(builder.toString()).queue();
		}
	}

	@Override
	public String getDescription() {
		return null;
	}

}
