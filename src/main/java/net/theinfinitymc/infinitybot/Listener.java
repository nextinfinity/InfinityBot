package net.theinfinitymc.infinitybot;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.theinfinitymc.infinitybot.modules.*;
import net.theinfinitymc.infinitybot.utils.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Listener extends ListenerAdapter {
	private JDA jda;
	
	private Audio audio;
	private Reddit reddit;
	private SoundCloud soundcloud;
	private Translate translate;
	private YouTube youtube;

	private Long start;
	private Integer messages = 0;
	
	public Listener(JDA jda){
		this.jda = jda;
		
		this.audio = new Audio();
		this.reddit = new Reddit();
		this.soundcloud = new SoundCloud();
		this.youtube = new YouTube();

		this.translate = new Translate();
		this.start = System.currentTimeMillis();
	}
	
	public Audio getAudio(){
		return audio;
	}
	
	@Override
	public void onMessageReceived(MessageReceivedEvent e){
		messages++;
		if(e.getAuthor().getId() == e.getJDA().getSelfUser().getId()) return;
		if(e.getAuthor().isBot()) return;
    	String[] args = e.getMessage().getContentStripped().split(" ");
    	String msg = args[0].toLowerCase();
    	String key = Config.getKey(e.getGuild());
    	if(msg.equals(key + "info")){
    		Message text = new MessageBuilder().append("__**Infinity Bot**__" 
    				+ "\n" + "Version: " + "@botVersion@"
    				+ "\n" + "API: JDA v" + "@jdaVersion@" + " w/ LavaPlayer v" + "@lavaVersion@"
    				+ "\n" + "Creator: NextInfinity").build();
    		e.getChannel().sendMessage(text).queue();
    	}
        if(msg.equals(key + "kick")){
        	if(e.getMember().hasPermission(Permission.KICK_MEMBERS)){
        		if(args.length > 1){
        			for(User user : e.getMessage().getMentionedUsers()){
        				e.getGuild().getController().kick(user.getId()).queue();
        			}
        		}
        	}
        }
        if(msg.equals(key + "translate")){
        	if(args.length >= 3){
        		int i = 3;
            	String m = args[2];
            	while(i < args.length){
            		m = m + " " + args[i];
            		i++;
            	}
            	final String query = m;
            	final String lang = args[1].toLowerCase();
            	InfinityBot.getThreadPool().execute(new Runnable(){
            		@Override
            		public void run(){
            			try {
            				e.getChannel().sendMessage(translate.translate(query, lang)).queue();
						} catch (Exception e) {
							e.printStackTrace();
						}
            		}
            	});
        	}
        }
        if(msg.equals(key + "ping")){
        	e.getChannel().sendMessage("Pong!").queue();
        }
        if(msg.equals(key + "help")){
        	if(args.length > 1){
        		if(args[1].equalsIgnoreCase("audio")){
        			e.getChannel().sendMessage("**Audio Commands**"
                    	+ "\n" + key + "play <link> - Adds a song to the queue (bot will join server if not already connected)"
							+ "\n" + key + "youtube <terms> - Adds the top result on YouTube for the given terms to the audio queue"
							+ "\n" + key + "soundcloud <terms> - Adds the top result on SoundCloud for the given terms to the audio queue"
                    	+ "\n" + key + "skip - Skips a song"
                    	+ "\n" + key + "stop - Stops audio and clears the queue"
                    	//+ "\n" + key + "nowplaying - See the currently playing song"
                    	+ "\n" + key + "volume <0-100> - Change audio volume"
                    	+ "\n" + key + "noot - NOOT NOOT"
                    	+ "\n" + key + "anothaone - SKRILLEX GIVES HIM ANOTHA ONE").queue();
        		}
        		if(args[1].equalsIgnoreCase("admin")){
        			e.getChannel().sendMessage("**Admin Commands**"
        				+ "\n" + key + "setkey <newkey> - Change the key for commands (Default: .)"
        				+ "\n" + "*These commands require the bot to have extra perms*"
                		+ "\n" + key + "kick <users> - Kicks tagged users"
                		+ "\n" + key + "ban <users> - Bans tagged users"
                		+ "\n" + key + "clear [amount] - Deletes the last [amount] of messages, 10 if no amount is specified").queue();
        		}
        		if(args[1].equalsIgnoreCase("reddit")){
        			e.getChannel().sendMessage("**Reddit Search**"
        				+ "\n" + key + "reddit <subreddit> [text] - Searches the subreddit (if no text is given, will take from front page"
        				+ "\n" + "*Modifiers - use: .reddit:<modifier>=<value>, defualt value is underlined*"
        				+ "\n" + "*For multiple modifiers, just add another :*"
        				+ "\n" + "sort (controversial, guilded, __hot__, new, rising, top) - how to sort results"
        				+ "\n" + "time (all, __day__, hour, month, week, year) - how far back to go"
        				+ "\n" + "amount (any number) - how many results to choose from").queue();
        		}
        	}else{
        		e.getChannel().sendMessage("__**Commands**__"
        			+ "\n" + key + "info - General bot info"
        			+ "\n" + key + "ping - Pong!"
        			+ "\n" + key + "translate <new lang> <english text> - Translate a message"
        			+ "\n" + key + "stats [@user] - Get a user's stats"
        			+ "\n**Admin Commands**: " + key + "help admin"
        			+ "\n**Audio Commands**: " + key + "help audio"
        			+ "\n**Reddit Search**: " + key + "help reddit").queue();
        	}
        }
        if(msg.startsWith(key + "reddit")){
        	String[] data = msg.split(":");
        	String sort = null;
        	String time = null;
        	Integer amount = null;
        	for(String entry : data){
        		if(entry.contains("=")){
        			String[] subdata = entry.split("=");
        			if(subdata[0].equalsIgnoreCase("sort")){
        				sort = subdata[1].toUpperCase();
        			}else if(subdata[0].equalsIgnoreCase("time")){
        				time = subdata[1].toUpperCase();
        			}else if(subdata[0].equalsIgnoreCase("amount")){
        				amount = Integer.valueOf(subdata[1]);
        			}
        		}
        	}
        	if(args.length > 1){
        		if(args.length == 2){
        			final String[] search = {sort, time};
        			final Integer count = amount;
                	InfinityBot.getThreadPool().execute(new Runnable(){
                		@Override
                		public void run(){
                			e.getChannel().sendMessage(reddit.subredditSearch(args[1].toLowerCase(), search[0], search[1], count)).queue();
                		}
                	});
        		}else{
        			int i = 3;
                	String m = args[2];
                	while(i < args.length){
                		m = m + " " + args[i];
                		i++;
                	}
        			final String[] search = {m, sort, time};
        			final Integer count = amount;
                	InfinityBot.getThreadPool().execute(new Runnable(){
                		@Override
                		public void run(){
                			e.getChannel().sendMessage(reddit.subredditSearch(args[1].toLowerCase(), search[0], search[1], search[2], count)).queue();
                		}
                	});
        		}
        	}
        }
        if(msg.equals(key + "ban")){
        	if(e.getMember().hasPermission(Permission.BAN_MEMBERS)){
        		if(args.length > 1){
        			for(User user : e.getMessage().getMentionedUsers()){
        				e.getGuild().getController().ban(user.getId(), 0).queue();
        			}
        		}
        	}
        }
        if(msg.equals(key + "play")){
        	if(args.length == 2){
        		String song = args[1];
				attemptAddToQueue(song, e.getGuild(), e.getTextChannel(), e.getAuthor());
        	}
        }
        if(msg.equals(key + "youtube")){
        	if(args.length >= 2){
				int i = 3;
				String m = args[2];
				while(i < args.length){
					m = m + " " + args[i];
					i++;
				}
				final String query = m;
				attemptAddToQueue(youtube.search(query), e.getGuild(), e.getTextChannel(), e.getAuthor());
			}
		}
		if(msg.equals(key + "soundcloud")){
			if(args.length >= 2){
				int i = 3;
				String m = args[2];
				while(i < args.length){
					m = m + " " + args[i];
					i++;
				}
				final String query = m;
				attemptAddToQueue(soundcloud.search(query), e.getGuild(), e.getTextChannel(), e.getAuthor());
			}
		}
        if(msg.equals(key + "anothaone")){
        	attemptAddToQueue("https://soundcloud.com/vagidictoris/skrillex-gives-him-another-one", e.getGuild(), e.getTextChannel(), e.getAuthor());
        }
        if(msg.equals(key + "noot")){
        	attemptAddToQueue("noot.mp3", e.getGuild(), e.getTextChannel(), e.getAuthor());
		}
        if(msg.equals(key + "volume")){
        	if(args.length == 2){
        		Integer vol = Integer.parseInt(args[1]);
        		if(vol > 1000){
        			vol = 1000;
				}
				if(vol < 0){
        			vol = 0;
				}
				audio.setVolume(vol, e.getGuild());
        		e.getChannel().sendMessage("Volume set to " + vol + "!").queue();
        	}
        }
        if(msg.equals(key + "skip")){
        	if(audio.isPlaying(e.getGuild())){
        		audio.skip(e.getGuild());
        	}else{
        		e.getChannel().sendMessage("Music is not playing!").queue();
        	}
        }
        if(msg.equals(key + "pause")){
        	audio.togglePause(e.getGuild(), e.getTextChannel());
        }
        if(msg.equals(key + "stop")){
        	audio.stop(e.getGuild());
        }
        if(msg.equals(key + "clear")){
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
        if(msg.equals(key + "setkey")){
        	if(e.getMember().hasPermission(Permission.ADMINISTRATOR)){
        		if(args.length == 2){
        			try{
        				Config.setKey(args[1], e.getGuild());
        			}catch(Exception ex){
        				ex.printStackTrace();
        			}
        		}
        	}
        }
        if(msg.equals(key + "stats")){
        	if(args.length > 1){
        		for(User u : e.getMessage().getMentionedUsers()){
        			e.getChannel().sendMessage(new MessageBuilder()
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
        		Integer conn = 0;
        		for(Guild guild : e.getJDA().getGuilds()){
        			if(guild.getAudioManager().isConnected()) conn++;
        		}
        		e.getChannel().sendMessage("Stats for " + e.getJDA().getSelfUser().getAsMention() + 
        				"\nServers Joined: " + e.getJDA().getGuilds().size() + 
        				"\nUnique Users: " + calculateUniqueUsers() +
        				"\nUptime: " + fTime +
        				"\nProcessed Messages: " + messages + 
        				"\nCurrent Audio Connections: " + conn).queue();
        	}
        }
        if(msg.equals(key + "admin")){
        	if(e.getAuthor().getId().equalsIgnoreCase(Config.getAdminId())){
        		if(args.length > 1){
        			if(args[1].equalsIgnoreCase("servers")){
        				String text = "__**Joined Servers**__";
        				for(Guild g : e.getJDA().getGuilds()){
        					text = text + "\n" + g.getName();
        				}
        				e.getChannel().sendMessage(text).queue();
        			}
        			if(args[1].equalsIgnoreCase("serverinfo")){
        				if(args.length >= 3){
        					int i = 3;
                    		String m = args[2];
                    		while(i < args.length){
                    			m = m + " " + args[i];
                    			i++;
                    		}
        					List<Guild> guilds = e.getJDA().getGuildsByName(m, true);
        					if(guilds != null && guilds.size() >= 1){
        						Guild g = guilds.get(0);
        						String text = "__**Server Info: " + g.getName() + "**__"
        								+ "\nOwner: " + g.getOwner().getEffectiveName()
        								+ "\nUsers: " + g.getMembers().size()
        								+ "\nText Channels: " + g.getTextChannels().size()
        								+ "\nVoice Channels: " + g.getVoiceChannels().size();
        						e.getChannel().sendMessage(text).queue();
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
        					List<Guild> guilds = e.getJDA().getGuildsByName(m, true);
        					if(guilds != null && guilds.size() >= 1){
        						Guild g = guilds.get(0);
        						String text = "__**Users in " + g.getName() + "**__";
        						for(Member mem: g.getMembers()){
        							text = text + "\n" + mem.getEffectiveName();
        						}
        						e.getChannel().sendMessage(text).queue();
        					}
        				}
        			}
        		}
        	}
        }
    }
	
	private void attemptAddToQueue(String song, Guild guild, TextChannel channel, User user){
		if(guild.getMember(user).getVoiceState().inVoiceChannel()){
    		if(!guild.getAudioManager().isConnected()){
    			try{
					guild.getAudioManager().openAudioConnection(guild.getMember(user).getVoiceState().getChannel());
				}catch(Exception ex){
					guild.getAudioManager().closeAudioConnection();
					channel.sendMessage("Error joining voice channel!").queue();
					return;
				}
			}
			audio.load(song, guild, channel);
    	}else{
    		channel.sendMessage("You must be in a voice channel to play a song!").queue();;
    	}
	}
	
	@Override
	public void onGuildJoin(GuildJoinEvent e){
		try{
			e.getGuild().getTextChannels().get(0).sendMessage("Hello! I am Infinity Bot. To see what I can do, type '" + Config.getKey(e.getGuild()) + "help'").queue();
		}catch(PermissionException ex){}
		/*try {
			JSONObject obj = new JSONObject().put("server_count", e.getJDA().getGuilds().size());
			Unirest.post("https://bots.discord.pw/api/bots/190953977129598976/stats")
					.header("Authorization", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySUQiOiI4MTEzMTg2NDI5NDY5NDkxMiIsInJhbmQiOjU0NSwiaWF0IjoxNDgzODIwMjM0fQ.c0p0zwJuwlug9okuxT0D07FQnwoLPzmoGqnfedSLxRY")
					.body(obj).asJson();
		} catch (UnirestException e1) {
			e1.printStackTrace();
		}*/
	}
	
	public void delete(Message m){
    	try{
    		m.delete().queue();
    	}catch(Exception e){}
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
