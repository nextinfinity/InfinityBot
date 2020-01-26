package net.theinfinitymc.infinitybot;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import net.dv8tion.jda.api.entities.TextChannel;

public class AudioListener extends AudioEventAdapter {
	private TextChannel c;
	private AudioPlayer p;
	private final BlockingQueue<AudioTrack> q = new LinkedBlockingQueue<>();

	AudioListener(TextChannel channel, AudioPlayer player) {
		c = channel;
		p = player;
	}

	public void queue(AudioTrack track) {
		if (!p.startTrack(track, true)) {
			q.offer(track);
		}
	}

	void playNext() {
		if (!p.startTrack(q.poll(), false)) {
			p.stopTrack();
			disconnect();
		}
	}

	@Override
	public void onTrackStart(AudioPlayer player, AudioTrack track) {
		c.sendMessage("Now Playing: " + track.getInfo().title).queue();
	}

	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
		if (endReason == AudioTrackEndReason.LOAD_FAILED) {
			c.sendMessage("Sorry, I had a problem playing that track.").queue();
		}
		if (endReason.mayStartNext) {
			playNext();
		} else if (endReason != AudioTrackEndReason.REPLACED) {
			disconnect();
		}
		// endReason == FINISHED: A track finished or died by an exception (mayStartNext = true).
		// endReason == LOAD_FAILED: Loading of a track failed (mayStartNext = true).
		// endReason == STOPPED: The player was stopped.
		// endReason == REPLACED: Another track started playing while this had not finished
		// endReason == CLEANUP: Player hasn't been queried for a while, if you want you can put a
		//                       clone of this back to your queue
	}

	@Override
	public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
		// An already playing track threw an exception (track end event will still be received separately)
		c.sendMessage("Sorry, I had a problem playing that track.").queue();
		System.out.println(exception.getMessage());
		exception.printStackTrace();
	}

	@Override
	public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
		// Audio track has been unable to provide us any audio, might want to just start a new track
		c.sendMessage("No audio received, skipping track.").queue();
		playNext();
	}

	@Override
	public void onPlayerPause(AudioPlayer player) {
		c.sendMessage("pause").queue();
	}

	void disconnect() {
		InfinityBot.getThreadPool().execute(() -> c.getGuild().getAudioManager().closeAudioConnection());
	}

	boolean hasNext() {
		return !q.isEmpty();
	}
}
