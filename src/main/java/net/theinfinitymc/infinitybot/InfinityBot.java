package net.theinfinitymc.infinitybot;

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import lombok.Value;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.io.IOException;
import java.util.Collections;

@Value
public class InfinityBot {
	JDA jda;
	AudioManager audioManager;
	Config config;

	public static void main(String[] args) {
		new InfinityBot();
	}

	InfinityBot() {
		try {
			this.config = new Config();
			CommandListener listener = new CommandListener();
			this.jda = JDABuilder.createDefault(config.getToken(),
							Collections.singletonList(GatewayIntent.GUILD_VOICE_STATES))
					.addEventListeners(listener)
					.setAudioSendFactory(new NativeAudioSendFactory())
					.build();
			this.audioManager = new AudioManager();
			listener.registerCommands(jda, audioManager, config);
			jda.getPresence().setActivity(Activity.watching("discord.gg/PvmhyMs for support"));
		} catch (Exception exception) {
			InstantiationError error = new InstantiationError("Failed to load InfinityBot.");
			error.setStackTrace(exception.getStackTrace());
			throw error;
		}
	}
}
