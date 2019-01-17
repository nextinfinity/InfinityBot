package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

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
		if(args.length > 0){
			for(User u : event.getMessage().getMentionedUsers()){
				event.getChannel().sendMessage(new MessageBuilder()
						.append("Stats for ").append(u)
						.append("\nUsername: " + u.getName())
						.append("\nID: " + u.getId())
						.append("\nDiscriminator: " + u.getDiscriminator())
						.append("\nAvatar URL: " + u.getAvatarUrl())
						.append("\nServers Shared: " + calculateSharedServers(u))
						.build()).queue();
			}
		}else{
			Long time = System.currentTimeMillis() - this.start;
			Long days = TimeUnit.MILLISECONDS.toDays(time);
			Long hours = TimeUnit.MILLISECONDS.toHours(time) - days*24;
			Long min = TimeUnit.MILLISECONDS.toMinutes(time) - days*1440 - hours*60;
			Long sec = TimeUnit.MILLISECONDS.toSeconds(time) - days*86400 - hours*3600 - min*60;
			String fTime = days + " days " + hours + ":" + min + ":" + sec;
			event.getChannel().sendMessage("Stats for " + event.getJDA().getSelfUser().getAsMention() +
					"\nServers Joined: " + event.getJDA().getGuilds().size() +
					"\nUnique Users: " + calculateUniqueUsers() +
					"\nUptime: " + fTime).queue();
		}
	}

	@Override
	public String getDescription() {
		return null;
	}

	private Integer calculateSharedServers(User u){
		Integer n = 0;
		for(Guild g : jda.getGuilds()){
			for(Member m : g.getMembers()){
				if(m.getUser() == u) n++;
				break;
			}
		}
		return n;
	}

	private Integer calculateUniqueUsers(){
		ArrayList<String> users = new ArrayList<String>();
		for(Guild g : jda.getGuilds()){
			for(Member m : g.getMembers()){
				User u = m.getUser();
				if(!users.contains(u.getId())){
					users.add(u.getId());
				}
			}
		}
		return users.size();
	}

}
