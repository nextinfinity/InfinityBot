package net.theinfinitymc.infinitybot;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.theinfinitymc.infinitybot.commands.*;
import net.theinfinitymc.infinitybot.utils.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Listener extends ListenerAdapter {
	private JDA jda;

	private Long start;
	private Integer messages = 0;

	private Map<String, Command> commands = new HashMap<>();
	
	public Listener(JDA jda){
		this.jda = jda;
		this.start = System.currentTimeMillis();

		registerCommands();
	}

	/**
	 * Load all commands into a map.
	 * I highly considered doing this with reflection, but decided that would be too error prone.
	 * However, this implementation may still be changed to be less tedious.
	 */
	private void registerCommands() {
		commands.put("admin", new Admin());
		commands.put("anothaone", new AnothaOne());
		commands.put("ban", new Ban());
		commands.put("clear", new Clear());
		commands.put("help", new Help());
		commands.put("info", new Info());
		commands.put("kick", new Kick());
		commands.put("noot", new Noot());
		commands.put("pause", new Pause());
		commands.put("ping", new Ping());
		commands.put("play", new Play());
		commands.put("reddit", new Reddit());
		commands.put("setkey", new SetKey());
		commands.put("skip", new Skip());
		commands.put("soundcloud", new Soundcloud());
		commands.put("stats", new Stats());
		commands.put("stop", new Stop());
		commands.put("translate", new Translate());
		commands.put("volume", new Volume());
		commands.put("youtube", new Youtube());
	}
	
	@Override
	public void onMessageReceived(MessageReceivedEvent event){
		messages++;
		if(event.getAuthor().getId() == event.getJDA().getSelfUser().getId()) return;
		if(event.getAuthor().isBot()) return;
    	String[] args = event.getMessage().getContentStripped().split(" ");
    	String key = Config.getKey(event.getGuild());
		String msg = args[0].toLowerCase();
		if (msg.startsWith(key)) {
			Command command = commands.get(msg.substring(key.length()));
			if (command != null) {
				command.execute(event, args);
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
	
}
