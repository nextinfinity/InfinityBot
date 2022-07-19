package net.theinfinitymc.infinitybot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.theinfinitymc.infinitybot.commands.*;

import java.util.HashMap;
import java.util.Map;

public class CommandListener extends ListenerAdapter {

	private final Map<String, Command> commands = new HashMap<>();

	/**
	 * Load all commands into a map.
	 */
	public void registerCommands(JDA jda, AudioManager audioManager, Config config) {
		registerCommand(jda, new Pause(audioManager));
		registerCommand(jda, new Play(audioManager));
		registerCommand(jda, new Queue(audioManager));
		registerCommand(jda, new Search(audioManager, config));
		registerCommand(jda, new Skip(audioManager));
		registerCommand(jda, new Stop(audioManager));
		registerCommand(jda, new Volume(audioManager));
	}

	private void registerCommand(JDA jda, Command command) {
		jda.upsertCommand(command.getName(), command.getDescription()).addOptions(command.getOptions()).queue();
		commands.put(command.getName(), command);
	}

	@Override
	public void onSlashCommand(SlashCommandEvent event) {
		Command command = commands.get(event.getName());
		if (command != null) {
			event.deferReply().queue();
			command.execute(event);
		}
	}

}
