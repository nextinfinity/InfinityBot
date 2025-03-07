package net.theinfinitymc.infinitybot;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.clients.*;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;

import java.util.*;

public class AudioManager {
	private final AudioPlayerManager audioPlayerManager;
	private final Map<Guild, GuildAudio> guildAudioMap;
	
	AudioManager(){
		this.guildAudioMap = new HashMap<>();
		this.audioPlayerManager = new DefaultAudioPlayerManager();

		// Register default sources, but replace the deprecated YT source with new version
		YoutubeAudioSourceManager youtubeSource = new YoutubeAudioSourceManager(true, new WebWithThumbnail(), new TvHtml5EmbeddedWithThumbnail());
		audioPlayerManager.registerSourceManager(youtubeSource);
		@SuppressWarnings("deprecation") Class<? extends AudioSourceManager> deprecatedYoutubeSource = com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager.class;
		AudioSourceManagers.registerRemoteSources(audioPlayerManager, deprecatedYoutubeSource);
	}

	public void tryAddToQueue(String song, Guild guild, MessageChannelUnion channel, User user, QueueCallback callback) {
		GuildAudio guildAudio = getGuildAudio(guild);
		if (connectToGuild(guildAudio, user, callback)) {
			loadSong(song, guildAudio, new GuildTrackData(user, channel, guild), callback);
		}
	}

	public boolean connectToGuild(GuildAudio guildAudio, User user, QueueCallback callback) {
		if (guildAudio.isConnected()) {
			// Don't hop around if already connected - stop and restart to change channels
			return true;
		}

		GuildVoiceState voiceState = Objects.requireNonNull(guildAudio.getGuild().getMember(user)).getVoiceState();
		if (voiceState != null && voiceState.inAudioChannel()) {
			try {
				guildAudio.connect(voiceState.getChannel());
			} catch (Exception e) {
				guildAudio.disconnect();
				callback.call(QueueCallback.QueueStatus.FAILURE_CONNECTION, Objects.requireNonNull(voiceState.getChannel()).getName());
				return false;
			}
		} else {
			callback.call(QueueCallback.QueueStatus.FAILURE_CHANNEL);
			return false;
		}

		return true;
	}

	private void loadSong(String song, GuildAudio guildAudio, GuildTrackData guildTrackData, QueueCallback callback) {
		audioPlayerManager.loadItem(song, new AudioLoadResultHandler() {
			boolean retry = false;
			@Override
			public void trackLoaded(AudioTrack track) {
				track.setUserData(guildTrackData);
				guildAudio.queue(track);
				if (guildAudio.queue(track)) {
					callback.call(QueueCallback.QueueStatus.SUCCESS, track.getInfo().title);
				} else {
					callback.call(QueueCallback.QueueStatus.FAILURE_QUEUE, song);
				}
			}

			@Override
			public void playlistLoaded(AudioPlaylist playlist) {
				if (retry)
				{
					for (AudioTrack track : playlist.getTracks()) {
						track.setUserData(guildTrackData);
						if (guildAudio.queue(track)) {
							callback.call(QueueCallback.QueueStatus.SUCCESS, track.getInfo().title);
							return;
						}
					}
					callback.call(QueueCallback.QueueStatus.FAILURE_QUEUE, song);
				} else {
					for (AudioTrack track : playlist.getTracks()) {
						track.setUserData(guildTrackData);
						if (!guildAudio.queue(track)) {
							callback.call(QueueCallback.QueueStatus.FAILURE_QUEUE, song);
							return;
						}
					}
					callback.call(QueueCallback.QueueStatus.SUCCESS, playlist.getName());
				}
			}

			@Override
			public void noMatches() {
				if (!retry)
				{
					// Retry policy: if unable to match, one retry with a youtube search for the input
					retry = true;
					audioPlayerManager.loadItem("ytsearch:" + song, this);
				} else {
					if (!guildAudio.isPlaying()) {
						guildAudio.disconnect();
					}
					callback.call(QueueCallback.QueueStatus.NO_MATCHES, song);
				}
			}

			@Override
			public void loadFailed(FriendlyException throwable) {
				if (!guildAudio.isPlaying()) {
					guildAudio.disconnect();
				}
				callback.call(QueueCallback.QueueStatus.FAILURE_LOAD, song);
			}
		});
	}
	
	public GuildAudio getGuildAudio(Guild guild) {
		if (!guildAudioMap.containsKey(guild)) {
			createGuildAudio(guild);
		}
		return guildAudioMap.get(guild);
	}

	public Collection<GuildAudio> getAllGuildAudios() {
		return guildAudioMap.values();
	}

	private void createGuildAudio(Guild guild){
		GuildAudio guildAudio = new GuildAudio(guild, audioPlayerManager.createPlayer());
		guildAudioMap.put(guild, guildAudio);
	}
}
