package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.theinfinitymc.infinitybot.AudioManager;
import net.theinfinitymc.infinitybot.Command;
import net.theinfinitymc.infinitybot.QueueCallback;

import java.util.List;
import java.util.Objects;

public class Play extends Command {

	public Play(AudioManager audioManager) {
		super(
				audioManager,
				"play",
				"Plays audio from the specified link in the user's voice channel.",
				List.of(
						new OptionData(OptionType.STRING, "link", "The link to play audio from (YouTube, SoundCloud, etc)")
								.setRequired(true)
				)
		);
	}

	public void execute(SlashCommandInteractionEvent event) {
		String link = Objects.requireNonNull(event.getOption("link")).getAsString();
		getAudioManager().tryAddToQueue(link, event.getGuild(), event.getChannel(), event.getUser(),
				new QueueCallback(event.getHook(), link));
	}

}
