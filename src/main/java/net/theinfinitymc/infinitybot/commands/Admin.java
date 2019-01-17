package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.theinfinitymc.infinitybot.utils.Config;

import java.util.List;

public class Admin implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		if(event.getAuthor().getId().equalsIgnoreCase(Config.getAdminId())){
			if(args.length > 1){
				if(args[1].equalsIgnoreCase("servers")){
					String text = "__**Joined Servers**__";
					for(Guild g : event.getJDA().getGuilds()){
						text = text + "\n" + g.getName();
					}
					event.getChannel().sendMessage(text).queue();
				}
				if(args[1].equalsIgnoreCase("serverinfo")){
					if(args.length >= 3){
						int i = 3;
						String m = args[2];
						while(i < args.length){
							m = m + " " + args[i];
							i++;
						}
						List<Guild> guilds = event.getJDA().getGuildsByName(m, true);
						if(guilds != null && guilds.size() >= 1){
							Guild g = guilds.get(0);
							String text = "__**Server Info: " + g.getName() + "**__"
									+ "\nOwner: " + g.getOwner().getEffectiveName()
									+ "\nUsers: " + g.getMembers().size()
									+ "\nText Channels: " + g.getTextChannels().size()
									+ "\nVoice Channels: " + g.getVoiceChannels().size();
							event.getChannel().sendMessage(text).queue();
						}
					}
				}
				if(args[1].equalsIgnoreCase("userlist")){
					if(args.length >= 3){
						int i = 3;
						String m = args[2];
						while(i < args.length){
							m = m + " " + args[i];
							i++;
						}
						List<Guild> guilds = event.getJDA().getGuildsByName(m, true);
						if(guilds != null && guilds.size() >= 1){
							Guild g = guilds.get(0);
							String text = "__**Users in " + g.getName() + "**__";
							for(Member mem: g.getMembers()){
								text = text + "\n" + mem.getEffectiveName();
							}
							event.getChannel().sendMessage(text).queue();
						}
					}
				}
			}
		}
	}

	@Override
	public String getDescription() {
		return null;
	}

}
