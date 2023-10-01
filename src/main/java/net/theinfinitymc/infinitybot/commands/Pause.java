package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.theinfinitymc.infinitybot.AudioManager;
import net.theinfinitymc.infinitybot.Command;

import java.util.List;

public class Pause extends Command {
	public enum PauseStatus {
		PAUSED, UNPAUSED, NO_MUSIC
	}

	public Pause(AudioManager audioManager) {
		super(
				audioManager,
				"pause",
				"Pause or unpause the currently playing music, if there is any.",
				List.of()
		);
	}

	public void execute(SlashCommandInteractionEvent event) {
		PauseStatus paused = getAudioManager().getGuildAudio(event.getGuild()).togglePause();
		switch (paused) {
			case PAUSED:
				event.getHook().editOriginal("Song paused.").queue();
				break;
			case UNPAUSED:
				event.getHook().editOriginal("Song resumed.").queue();
				break;
			case NO_MUSIC:
				event.getHook().editOriginal("Music was not playing.").queue();
				break;
		}
	}

}
