package net.theinfinitymc.infinitybot;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.theinfinitymc.infinitybot.utils.AudioPlayerSendHandler;

import java.util.HashMap;

public class Audio {
	private AudioPlayerManager playerManager;
	private HashMap<Guild, AudioListener> listeners = new HashMap<>();
	
	Audio(){
		playerManager = new DefaultAudioPlayerManager();
		AudioSourceManagers.registerRemoteSources(playerManager);
		playerManager.setFrameBufferDuration(10000);
		playerManager.setTrackStuckThreshold(5000);
	}
	
	private void load(String link, Guild g, TextChannel c){
		if(getPlayer(g) == null) createPlayer(g, c);
		playerManager.loadItem(link, new AudioLoadResultHandler() {
			  @Override
			  public void trackLoaded(AudioTrack track) {
				  c.sendMessage("Song added to queue!").queue();
			    listeners.get(g).queue(track);
			  }

			  @Override
			  public void playlistLoaded(AudioPlaylist playlist) {
				  c.sendMessage("Playlist added to queue!").queue();
			    for (AudioTrack track : playlist.getTracks()) {
			    	listeners.get(g).queue(track);
			    }
			  }

			  @Override
			  public void noMatches() {
				  c.sendMessage("Sorry, I had a problem playing that track.").queue();
				  if(!listeners.get(g).hasNext()){
				  		listeners.get(g).disconnect();
				  }
			  }

			  @Override
			  public void loadFailed(FriendlyException throwable) {
			    c.sendMessage("Sorry, I had a problem playing that track.").queue();
			    if(!listeners.get(g).hasNext()){
					g.getAudioManager().closeAudioConnection();
				}
			  }
			});
	}

	public void addToQueue(String song, Guild guild, TextChannel channel, User user){
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
			load(song, guild, channel);
		}else{
			channel.sendMessage("You must be in a voice channel to play a song!").queue();
		}
	}

	public void setVolume(Integer vol, Guild g){
		if(isPlaying(g)) getPlayer(g).setVolume(vol);
	}
	
	public void togglePause(Guild g, TextChannel c){
		AudioPlayer player = getPlayer(g);
		if(player.isPaused()){
			player.setPaused(false);
		}else if(player.getPlayingTrack() != null){
			player.setPaused(true);
		}else{
			c.sendMessage("Music is not playing or paused!").queue();
		}
	}
	
	public void skip(Guild g){
		listeners.get(g).playNext();
	}
	
	public void stop(Guild g){
		AudioPlayer player = getPlayer(g);
		player.stopTrack();
		g.getAudioManager().closeAudioConnection();
	}
	
	public boolean isPlaying(Guild g){
		return getPlayer(g) != null && getPlayer(g).getPlayingTrack() != null;
	}

	private AudioPlayer getPlayer(Guild g){
		if(g.getAudioManager().getSendingHandler() == null) return null;
		return ((AudioPlayerSendHandler) g.getAudioManager().getSendingHandler()).getPlayer();
	}

	private AudioPlayer createPlayer(Guild g, TextChannel c){
		AudioPlayer player = playerManager.createPlayer();
		g.getAudioManager().setSendingHandler(new AudioPlayerSendHandler(player));
		AudioListener listener = new AudioListener(c, player);
		player.addListener(listener);
		listeners.put(g, listener);
		player.setVolume(5);
		return player;
	}
}
