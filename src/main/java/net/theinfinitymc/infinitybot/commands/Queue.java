package net.theinfinitymc.infinitybot.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.theinfinitymc.infinitybot.AudioManager;
import net.theinfinitymc.infinitybot.Command;
import net.theinfinitymc.infinitybot.GuildAudio;

import java.util.List;

public class Queue extends Command {

    public Queue(AudioManager audioManager) {
        super(
                audioManager,
                "queue",
                "View the current audio track queue.",
                List.of()
        );
    }

    public void execute(SlashCommandInteractionEvent event) {
        GuildAudio guildAudio = getAudioManager().getGuildAudio(event.getGuild());
        if (guildAudio.hasNext()) {
            StringBuilder response = new StringBuilder("__Queue__");
            AudioTrack[] trackList = new AudioTrack[guildAudio.getQueue().size()];
            guildAudio.getQueue().toArray(trackList);
            for (int i = 1; i <= trackList.length; i++) {
                AudioTrackInfo trackInfo = trackList[i - 1].getInfo();
                long length = trackInfo.length / 1000;
                response.append(String.format("\n%d. %s (%d:%02d)", i, trackInfo.title, length/ 60, length % 60));
            }
            event.getHook().editOriginal(response.toString()).queue();
        } else {
            event.getHook().editOriginal("The queue is empty.").queue();
        }
    }

}
