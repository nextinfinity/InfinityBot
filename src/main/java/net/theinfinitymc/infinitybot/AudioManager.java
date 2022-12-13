package net.theinfinitymc.infinitybot;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

import java.util.HashMap;
import java.util.Map;

public class AudioManager {
	private final AudioPlayerManager audioPlayerManager;
	private final Map<Guild, GuildAudio> guildAudioMap;
	
	AudioManager(){
		this.audioPlayerManager = new DefaultAudioPlayerManager();
		AudioSourceManagers.registerRemoteSources(audioPlayerManager);
		audioPlayerManager.setFrameBufferDuration(10000);
		audioPlayerManager.setTrackStuckThreshold(5000);
		this.guildAudioMap = new HashMap<>();
	}

	public void tryAddToQueue(String song, Guild guild, TextChannel channel, User user, QueueCallback callback) {
		GuildAudio guildAudio = getGuildAudio(guild);
		if (!guildAudio.isConnected()) {
			GuildVoiceState voiceState = guild.getMember(user).getVoiceState();
			if (voiceState.inVoiceChannel()) {
				try {
					guildAudio.connect(voiceState.getChannel());
				} catch (Exception e) {
					guildAudio.disconnect();
					callback.call(QueueCallback.QueueStatus.FAILURE_CONNECTION);
				}
			} else {
				callback.call(QueueCallback.QueueStatus.FAILURE_CHANNEL);
			}
		}
		loadSong(song, guild, channel, callback);
	}

	private void loadSong(String song, Guild guild, TextChannel channel, QueueCallback callback){
		GuildAudio guildAudio = getGuildAudio(guild);
		audioPlayerManager.loadItem(song, new AudioLoadResultHandler() {
			@Override
			public void trackLoaded(AudioTrack track) {
				track.setUserData(channel);
				guildAudio.queue(track);
				callback.call(QueueCallback.QueueStatus.SUCCESS);
			}

			@Override
			public void playlistLoaded(AudioPlaylist playlist) {
				for (AudioTrack track : playlist.getTracks()) {
					track.setUserData(channel);
					guildAudio.queue(track);
				}
				callback.call(QueueCallback.QueueStatus.SUCCESS);
			}

			@Override
			public void noMatches() {
				if (!guildAudio.isPlaying()) {
					guildAudio.disconnect();
				}
				callback.call(QueueCallback.QueueStatus.FAILURE_LOAD);
			}

			@Override
			public void loadFailed(FriendlyException throwable) {
				if (!guildAudio.isPlaying()) {
					guildAudio.disconnect();
				}
				callback.call(QueueCallback.QueueStatus.FAILURE_LOAD);
			}
		});
	}
	
	public GuildAudio getGuildAudio(Guild guild) {
		if (!guildAudioMap.containsKey(guild)) {
			createGuildAudio(guild);
		}
		return guildAudioMap.get(guild);
	}

	private void createGuildAudio(Guild guild){
		GuildAudio guildAudio = new GuildAudio(guild, audioPlayerManager.createPlayer());
		guildAudioMap.put(guild, guildAudio);
	}
}
