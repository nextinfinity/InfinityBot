package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.theinfinitymc.infinitybot.AudioManager;
import net.theinfinitymc.infinitybot.Command;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class Timer extends Command {

	public Timer(AudioManager audioManager) {
		super(
			audioManager,
			"timer",
			"Sets a timer for the specified time. Will pull users back to channel at end if specified.",
			List.of(
				new OptionData(OptionType.STRING, "name", "Name for the timer")
					.setRequired(true),
				new OptionData(OptionType.CHANNEL, "text-channel", "Text channel to post timer in")
					.setRequired(true).setChannelTypes(ChannelType.TEXT),
				new OptionData(OptionType.INTEGER, "length", "The length of the timer, in minutes")
					.setRequired(true).setMinValue(1),
				new OptionData(OptionType.INTEGER, "notify-interval", "How often to post separate update notifications, in minutes")
					.setRequired(false).setMinValue(1),
				new OptionData(OptionType.BOOLEAN, "one-minute-warning", "Whether or not to notify with one minute remaining")
					.setRequired(false),
				new OptionData(OptionType.MENTIONABLE, "notify-mention", "Who to mention for updates")
					.setRequired(false),
				new OptionData(OptionType.CHANNEL, "return-voice-channel", "Channel to send all voice users to when the timer completes")
					.setRequired(false).setChannelTypes(ChannelType.VOICE)
			)
		);
	}

	@Override
	public DefaultMemberPermissions getDefaultPermissions() {
		return DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR);
	}

	public void execute(SlashCommandInteractionEvent event) {
		if (!event.isFromGuild()) {
			event.getHook().editOriginal("Timers can only be created in a server.").queue();
			return;
		}

		// Required parameters

		final String timerName = Objects.requireNonNull(event.getOption("name")).getAsString();
		final TextChannel textChannel = Objects.requireNonNull(event.getOption("text-channel")).getAsChannel().asTextChannel();
		final long timerLength = Objects.requireNonNull(event.getOption("length")).getAsLong();

		// Optional parameters

		final OptionMapping notifyIntervalOption = event.getOption("notify-interval");
		final long notifyInterval = notifyIntervalOption != null ? notifyIntervalOption.getAsLong() : timerLength;

		final OptionMapping oneMinuteWarningOption = event.getOption("one-minute-warning");
		boolean sendOneMinuteWarning = oneMinuteWarningOption != null ? oneMinuteWarningOption.getAsBoolean() : false;

		final OptionMapping mentionOption = event.getOption("notify-mention");
		final String mention = mentionOption != null ? mentionOption.getAsMentionable().getAsMention() + " " : "";

		final OptionMapping voiceChannelOption = event.getOption("return-voice-channel");
		final VoiceChannel voiceChannel = voiceChannelOption != null ? voiceChannelOption.getAsChannel().asVoiceChannel() : null;

		// Validate bot permissions before sending or scheduling any notices.
		// Caller access is controlled by Discord's command permissions, not here.
		final Member self = Objects.requireNonNull(event.getGuild()).getSelfMember();
		if (!self.hasPermission(textChannel, Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND)) {
			event.getHook().editOriginal("I need View Channel and Send Messages in the timer's text channel.").queue();
			return;
		}
		if (mentionOption != null && mentionOption.getAsMentionable() instanceof Role role
				&& !role.isMentionable() && !self.hasPermission(textChannel, Permission.MESSAGE_MENTION_EVERYONE)) {
			event.getHook().editOriginal("I need Mention Everyone permission in the timer's text channel to mention that role.").queue();
			return;
		}
		if (voiceChannel != null) {
			if (!self.hasPermission(voiceChannel, Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT)) {
				event.getHook().editOriginal("I need View Channel and Connect in the return voice channel.").queue();
				return;
			}
			// Members may change channels during the timer, so check every possible source,
			// including stage channels, rather than just currently occupied channels.
			for (GuildChannel channel : Objects.requireNonNull(event.getGuild()).getChannels()) {
				if (channel instanceof AudioChannel && !self.hasPermission(channel, Permission.VOICE_MOVE_OTHERS)) {
					event.getHook().editOriginal("I need Move Members in every voice and stage channel to return all users.").queue();
					return;
				}
			}
		}

		// Reply to the actual slash command

		event.getHook().editOriginal("Creating timer...").queue();

		// Send start of timer message

		textChannel.sendMessage(mention + "Timer for *" + timerName + "* set for **" + timerLength + "** minutes").queue();

		// Schedule interval messages

		for (long notifyTime = notifyInterval; notifyTime < timerLength; notifyTime += notifyInterval) {
			if (notifyTime == timerLength - 1) {
				sendOneMinuteWarning = true;
				break;
			}

			textChannel.sendMessage(mention + "**" + (timerLength - notifyTime) + "** minutes remaining for *" + timerName + "*").queueAfter(notifyTime, TimeUnit.MINUTES);
		}

		// Schedule one-minute warning

		if (sendOneMinuteWarning && timerLength > 1) {
			textChannel.sendMessage(mention + "Only ***1*** minute remaining for *" + timerName + "*").queueAfter(timerLength - 1, TimeUnit.MINUTES);
		}

		// Schedule final message and voice channel movement

		textChannel.sendMessage(mention + "*" + timerName + "* complete!").onSuccess(a -> {
			if (voiceChannel != null) {
				final Guild guild = voiceChannel.getGuild();
				for (Member member : voiceChannel.getGuild().getMembers()) {
					GuildVoiceState memberVoiceState = Objects.requireNonNull(member.getVoiceState());
					if (memberVoiceState.inAudioChannel()) {
						guild.moveVoiceMember(member, voiceChannel).queueAfter(3, TimeUnit.SECONDS);
					}
				}
			}
		}).queueAfter(timerLength, TimeUnit.MINUTES);
	}

}
