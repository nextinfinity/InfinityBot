package net.theinfinitymc.infinitybot;

import lombok.Value;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

@Value
public class GuildTrackData {
    User adder;
    TextChannel channel;
}
