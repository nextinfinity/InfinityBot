package net.theinfinitymc.infinitybot;

import club.minnced.discord.jdave.interop.JDaveSessionFactory;
import net.dv8tion.jda.api.audio.AudioModuleConfig;
import lombok.Value;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;

import net.theinfinitymc.infinitybot.polls.PollListener;

import java.nio.file.Path;
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
			// Validate YouTube configuration before opening the Discord connection.
			this.audioManager = new AudioManager();
			CommandListener listener = new CommandListener();
			String pollDatabase = System.getenv("POLL_DATABASE_PATH");
			PollListener polls = new PollListener(Path.of(pollDatabase == null || pollDatabase.isBlank()
					? "data/polls.sqlite" : pollDatabase));
			this.jda = JDABuilder.createDefault(System.getenv("DISCORD_BOT_TOKEN"),
							Collections.singletonList(GatewayIntent.GUILD_VOICE_STATES))
					.setAudioModuleConfig(new AudioModuleConfig()
							.withDaveSessionFactory(new JDaveSessionFactory()))
					.addEventListeners(listener, polls)
					.build();
			listener.registerCommands(jda, audioManager);
			updateActivity();
			HealthCheck.start(jda);
		} catch (Exception exception) {
			throw new IllegalStateException("Failed to load InfinityBot.", exception);
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
			jda.getPresence().setActivity(activities.get(ThreadLocalRandom.current().nextInt(activities.size())));
		} else {
			jda.getPresence().setActivity(Activity.watching("discord.gg/PvmhyMs for support"));
		}
	}
}
