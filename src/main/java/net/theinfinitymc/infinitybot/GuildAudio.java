package net.theinfinitymc.infinitybot;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.theinfinitymc.infinitybot.commands.Pause;

import java.awt.*;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Value
@EqualsAndHashCode(callSuper=false)
public class GuildAudio extends AudioEventAdapter {
	Guild guild;
	AudioPlayer player;
	BlockingQueue<AudioTrack> queue;

	GuildAudio(Guild guild, AudioPlayer player) {
		this.guild = guild;
		this.player = player;
		this.queue = new LinkedBlockingQueue<>();
		guild.getAudioManager().setSendingHandler(new AudioPlayerSendHandler(player));
		player.addListener(this);
		player.setVolume(20);
	}

	public boolean queue(AudioTrack track) {
		if (player.startTrack(track, true)) {
			return true;
		}

		return queue.offer(track);
	}

	public Pause.PauseStatus togglePause() {
		if (player.getPlayingTrack() == null) {
			return Pause.PauseStatus.NO_MUSIC;
		}
		if (!player.isPaused()) {
			player.setPaused(true);
			return Pause.PauseStatus.PAUSED;
		} else {
			player.setPaused(false);
			return Pause.PauseStatus.UNPAUSED;
		}
	}

	public boolean skip() {
		if (!isPlaying()) {
			return false;
		}
		playNext();
		return true;
	}

	private void playNext() {
		if (!hasNext() || !player.startTrack(queue.poll(), false)) {
			player.stopTrack();
			disconnect();
		}
	}

	public boolean stop() {
		if (!isPlaying()) {
			return false;
		}
		player.stopTrack();
		disconnect();
		queue.clear();
		return true;
	}

	public void setVolume(int volume) {
		player.setVolume(volume);
	}

	public boolean isPlaying() {
		return player.getPlayingTrack() != null;
	}

	public boolean hasNext() {
		return !queue.isEmpty();
	}

	public void connect(AudioChannelUnion channel) {
		guild.getAudioManager().openAudioConnection(channel);
	}

	public void disconnect() {
		guild.getAudioManager().closeAudioConnection();
		InfinityBot.instance.updateActivity();
	}

	public boolean isConnected() {
		return guild.getAudioManager().isConnected();
	}

	@Override
	public void onTrackStart(AudioPlayer player, AudioTrack track) {
		AudioTrackInfo trackInfo = track.getInfo();
		GuildTrackData trackData = (GuildTrackData) track.getUserData();
		EmbedBuilder embed = new EmbedBuilder();
		embed.setTitle(trackInfo.title, trackInfo.uri);
		embed.setColor(Color.red);
		embed.setAuthor("Now Playing", null, "https://static3.depositphotos.com/1001442/197/i/600/depositphotos_1970119-stock-photo-music-record.jpg");
		embed.setImage(track.getInfo().artworkUrl);
		embed.setFooter("Requested by " + trackData.getAdderName(), trackData.getAdderAvatar());
		embed.setTimestamp(Instant.now());
		Message message = trackData.getChannel().sendMessageEmbeds(embed.build()).complete();
		track.setUserData(message);

		InfinityBot.instance.updateActivity();
	}

	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
		if (endReason == AudioTrackEndReason.LOAD_FAILED) {
			((Message) track.getUserData()).reply("Unable to play song - proceeding to next available.").queue();
		}
		if (endReason.mayStartNext) {
			playNext();
		} else if (endReason != AudioTrackEndReason.REPLACED) {
			disconnect();
		}
	}

	@Override
	public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
		((Message) track.getUserData()).reply("No audio detected, skipping track.").queue();
		playNext();
	}
}
