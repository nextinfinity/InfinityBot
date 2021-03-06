package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class Stats implements Command {

	private final long start;
	private final JDA jda;

	public Stats(JDA jda) {
		this.start = System.currentTimeMillis();
		this.jda = jda;
	}

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		if (args.length > 0) {
			for (User u : event.getMessage().getMentionedUsers()) {
				event.getChannel().sendMessage(new MessageBuilder()
						.append("Stats for ").append(u)
						.append("\nUsername: ").append(u.getName())
						.append("\nID: ").append(u.getId())
						.append("\nDiscriminator: ").append(u.getDiscriminator())
						.append("\nAvatar URL: ").append(u.getAvatarUrl())
						.append("\nServers Shared: ").append(String.valueOf(calculateSharedServers(u)))
						.build()).queue();
			}
		} else {
			long time = System.currentTimeMillis() - this.start;
			long days = TimeUnit.MILLISECONDS.toDays(time);
			long hours = TimeUnit.MILLISECONDS.toHours(time) - days * 24;
			long min = TimeUnit.MILLISECONDS.toMinutes(time) - days * 1440 - hours * 60;
			long sec = TimeUnit.MILLISECONDS.toSeconds(time) - days * 86400 - hours * 3600 - min * 60;
			String fTime = days + " days " + hours + ":" + min + ":" + sec;
			event.getChannel().sendMessage("Stats for " + event.getJDA().getSelfUser().getAsMention() +
					"\nServers Joined: " + event.getJDA().getGuilds().size() +
					"\nUnique Users: " + calculateUniqueUsers() +
					"\nUptime: " + fTime).queue();
		}
	}

	@Override
	public String getDescription() {
		return "Displays stats for all mentioned users.";
	}

	private Integer calculateSharedServers(User u) {
		Integer n = 0;
		for (Guild g : jda.getGuilds()) {
			for (Member m : g.getMembers()) {
				if (m.getUser() == u) {
					n++;
					break;
				}
			}
		}
		return n;
	}

	private Integer calculateUniqueUsers() {
		ArrayList<String> users = new ArrayList<>();
		for (Guild g : jda.getGuilds()) {
			for (Member m : g.getMembers()) {
				User u = m.getUser();
				if (!users.contains(u.getId())) {
					users.add(u.getId());
				}
			}
		}
		return users.size();
	}

}
