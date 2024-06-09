package net.theinfinitymc.infinitybot;

import lombok.Value;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;

import java.util.Objects;

@Value
public class GuildTrackData {
    User adder;
    MessageChannelUnion channel;
    Guild guild;

    public String getAdderName() {
       return getAdderMember().getEffectiveName();
    }

    public String getAdderAvatar() {
        return getAdderMember().getEffectiveAvatarUrl();
    }

    private Member getAdderMember() {
        return guild.getMember(adder);
    }
}
