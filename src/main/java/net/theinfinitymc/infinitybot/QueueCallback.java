package net.theinfinitymc.infinitybot;

import lombok.Value;
import net.dv8tion.jda.api.interactions.InteractionHook;

@Value
public class QueueCallback {
    public enum QueueStatus {
        SUCCESS, FAILURE_CHANNEL, FAILURE_CONNECTION, FAILURE_LOAD
    }

    InteractionHook hook;
    String song;

    public void call(QueueStatus status) {
        switch (status) {
            case SUCCESS:
                hook.editOriginalFormat("Added to the queue.").queue();
                break;
            case FAILURE_CHANNEL:
                hook.editOriginal("You must be in a voice channel for the bot to join.").queue();
                break;
            case FAILURE_CONNECTION:
                hook.editOriginal("Unable to join your voice channel.").queue();
                break;
            case FAILURE_LOAD:
                hook.editOriginalFormat("Unable to load track.").queue();
                break;
        }
    }

}
