package net.theinfinitymc.infinitybot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.theinfinitymc.infinitybot.commands.*;
import net.theinfinitymc.infinitybot.utils.Config;

import java.util.*;

public class Listener extends ListenerAdapter {
	private JDA jda;

	private Map<String, Command> commands = new HashMap<>();

	Listener(JDA jda) {
		this.jda = jda;

		registerCommands();
	}

	/**
	 * Load all commands into a map.
	 */
	private void registerCommands() {
		commands.put("admin", new Admin());
		commands.put("anothaone", new AnothaOne());
		commands.put("ban", new Ban());
		commands.put("clear", new Clear());
		commands.put("help", new Help(this));
		commands.put("info", new Info());
		commands.put("kick", new Kick());
		commands.put("noot", new Noot());
		commands.put("pause", new Pause());
		commands.put("ping", new Ping());
		commands.put("play", new Play());
		commands.put("reddit", new Reddit());
		commands.put("setkey", new SetKey());
		commands.put("skip", new Skip());
		commands.put("soundcloud", new SoundCloud());
		commands.put("stats", new Stats(jda));
		commands.put("stop", new Stop());
		commands.put("translate", new Translate());
		commands.put("volume", new Volume());
		commands.put("youtube", new YouTube());
	}

	public Map<String, Command> getCommands() {
		return commands;
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if (event.getAuthor().getId().equals(event.getJDA().getSelfUser().getId())) return;
		if (event.getAuthor().isBot()) return;
		String[] args = event.getMessage().getContentStripped().split(" ");
		String key = Config.getKey(event.getGuild());
		String msg = args[0].toLowerCase();
		if (msg.startsWith(key)) {
			Command command = commands.get(msg.substring(key.length()));
			if (command != null) {
				command.execute(event, Arrays.copyOfRange(args, 1, args.length));
			}
		}
	}

	@Override
	public void onGuildJoin(GuildJoinEvent e) {
		try {
			e.getGuild().getTextChannels().get(0).sendMessage("Hello! I am Infinity Bot. To see what I can do, type '" + Config.getKey(e.getGuild()) + "help'").queue();
		} catch (PermissionException ex) {
			//TODO PermissionException
		}
	}

}
