package net.theinfinitymc.infinitybot;

import lombok.Value;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;

@Value
public class GuildTrackData {
    User adder;
    MessageChannelUnion channel;
}
