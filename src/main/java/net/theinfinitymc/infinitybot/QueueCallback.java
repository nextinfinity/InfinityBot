package net.theinfinitymc.infinitybot;

import lombok.Value;
import net.dv8tion.jda.api.interactions.InteractionHook;

@Value
public class QueueCallback {
    public enum QueueStatus {
        SUCCESS, NO_MATCHES, FAILURE_CHANNEL, FAILURE_CONNECTION, FAILURE_LOAD, FAILURE_QUEUE
    }

    InteractionHook hook;
    String song;

    public void call(QueueStatus status) {
        call(status, "");
    }

    public void call(QueueStatus status, String info) {
        switch (status) {
            case SUCCESS:
                hook.editOriginalFormat("Added \"%s\" to the queue!", info).queue();
                break;
            case NO_MATCHES:
                hook.editOriginalFormat("Unable to find any matches for \"%s\".", info).queue();
            case FAILURE_CHANNEL:
                hook.editOriginal("You must be in a voice channel for the bot to join.").queue();
                break;
            case FAILURE_CONNECTION:
                hook.editOriginalFormat("Unable to join voice channel \"%s\".", info).queue();
                break;
            case FAILURE_LOAD:
                hook.editOriginalFormat("Found a match for \"%s\", but was unable to load the audio.", info).queue();
                break;
            case FAILURE_QUEUE:
                hook.editOriginalFormat("Found a match for \"%s\", but was unable to add audio to queue.", info).queue();
                break;
        }
    }

}
