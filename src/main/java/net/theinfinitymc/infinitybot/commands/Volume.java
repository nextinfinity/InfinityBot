package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.theinfinitymc.infinitybot.AudioManager;
import net.theinfinitymc.infinitybot.Command;

import java.util.List;
import java.util.Objects;

public class Volume extends Command {

	public Volume(AudioManager audioManager) {
		super(
				audioManager,
				"volume",
				"Sets the volume of the currently playing music.",
				List.of(
					new OptionData(OptionType.INTEGER, "volume", "The volume to set")
							.setRequired(true).setMinValue(0).setMaxValue(100)
				)
		);
	}

	public void execute(SlashCommandEvent event) {
		int volume = (int) Objects.requireNonNull(event.getOption("volume")).getAsLong();
		getAudioManager().getGuildAudio(event.getGuild(), event.getTextChannel()).setVolume(volume);
		event.getHook().editOriginalFormat("Volume set to %d.", volume).queue();
	}

}
