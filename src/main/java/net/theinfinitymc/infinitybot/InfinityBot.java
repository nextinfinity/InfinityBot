package net.theinfinitymc.infinitybot;

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import lombok.Value;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Value
public class InfinityBot {
	JDA jda;
	AudioManager audioManager;
	static InfinityBot instance;

	public static void main(String[] args) {
		instance = new InfinityBot();
	}

	InfinityBot() {
		try {
			CommandListener listener = new CommandListener();
			this.jda = JDABuilder.createDefault(System.getenv("DISCORD_BOT_TOKEN"),
							Collections.singletonList(GatewayIntent.GUILD_VOICE_STATES))
					.addEventListeners(listener)
					.setAudioSendFactory(new NativeAudioSendFactory())
					.build();
			this.audioManager = new AudioManager();
			listener.registerCommands(jda, audioManager);
			updateActivity();
		} catch (Exception exception) {
			InstantiationError error = new InstantiationError("Failed to load InfinityBot.");
			error.setStackTrace(exception.getStackTrace());
			throw error;
		}
	}

	public void updateActivity() {
		List<Activity> activities = new ArrayList<>();
		for (GuildAudio guildAudio : audioManager.getAllGuildAudios()) {
			if (guildAudio.isPlaying()) {
				activities.add(Activity.listening(guildAudio.getPlayer().getPlayingTrack().getInfo().title));
			}
		}

		if (!activities.isEmpty()) {
			jda.getPresence().setActivity(activities.get(ThreadLocalRandom.current().nextInt() % activities.size()));
		} else {
			jda.getPresence().setActivity(Activity.watching("discord.gg/PvmhyMs for support"));
		}
	}
}
