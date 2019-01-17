package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.theinfinitymc.infinitybot.commands.admin.ServerInfo;
import net.theinfinitymc.infinitybot.commands.admin.Servers;
import net.theinfinitymc.infinitybot.commands.admin.UserList;
import net.theinfinitymc.infinitybot.utils.Config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Admin implements Command {

	private final Map<String, Command> adminCommands = new HashMap<>();

	public Admin() {
		loadCommands();
	}

	private void loadCommands() {
		adminCommands.put("serverinfo", new ServerInfo());
		adminCommands.put("servers", new Servers());
		adminCommands.put("userlist", new UserList());
	}

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		if(event.getAuthor().getId().equalsIgnoreCase(Config.getAdminId())){
			if(args.length > 1){
				String[] filteredArgs = Arrays.copyOfRange(args, 1, args.length);
				Command command = adminCommands.get(args[0].toLowerCase());
				if (command != null) {
					command.execute(event, filteredArgs);
				}
			}
		}
	}

	@Override
	public String getDescription() {
		return null;
	}

}
