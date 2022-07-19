package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.theinfinitymc.infinitybot.AudioManager;
import net.theinfinitymc.infinitybot.Command;

import java.util.List;

public class Stop extends Command {

	public Stop(AudioManager audioManager) {
		super(
				audioManager,
				"stop",
				"Stops the currently playing music, if there is any.",
				List.of()
		);
	}

	public void execute(SlashCommandEvent event) {
		boolean stopped = getAudioManager().getGuildAudio(event.getGuild(), event.getTextChannel()).stop();
		if (stopped) {
			event.getHook().editOriginal("Stopped music.").queue();
		} else {
			event.getHook().editOriginal("Music was not playing.").queue();
		}
	}

}
