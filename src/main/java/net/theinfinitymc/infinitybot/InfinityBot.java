package net.theinfinitymc.infinitybot;

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import lombok.Value;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.Collections;

@Value
public class InfinityBot {
	JDA jda;
	AudioManager audioManager;

	public static void main(String[] args) {
		new InfinityBot();
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
			jda.getPresence().setActivity(Activity.watching("discord.gg/PvmhyMs for support"));
		} catch (Exception exception) {
			InstantiationError error = new InstantiationError("Failed to load InfinityBot.");
			error.setStackTrace(exception.getStackTrace());
			throw error;
		}
	}
}
