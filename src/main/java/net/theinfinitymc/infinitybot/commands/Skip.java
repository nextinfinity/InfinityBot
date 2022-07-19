package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.theinfinitymc.infinitybot.AudioManager;
import net.theinfinitymc.infinitybot.Command;

import java.util.List;

public class Skip extends Command {

	public Skip(AudioManager audioManager) {
		super(
				audioManager,
				"skip",
				"Skips the currently playing music, if there is any.",
				List.of()
		);
	}

	public void execute(SlashCommandEvent event) {
		boolean skipped = getAudioManager().getGuildAudio(event.getGuild(), event.getTextChannel()).skip();
		if (skipped) {
			event.getHook().editOriginal("Skipped song.").queue();
		} else {
			event.getHook().editOriginal("Music was not playing.").queue();
		}
	}

}
