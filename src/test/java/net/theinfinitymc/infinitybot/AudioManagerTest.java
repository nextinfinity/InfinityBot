package net.theinfinitymc.infinitybot;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static net.theinfinitymc.infinitybot.QueueCallback.QueueStatus.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AudioManagerTest {
    private final AudioPlayerManager loader = mock(AudioPlayerManager.class);
    private final AudioManager manager = spy(new AudioManager(loader));
    private final Guild guild = mock(Guild.class, RETURNS_DEEP_STUBS);
    private final GuildAudio audio = mock(GuildAudio.class);
    private final User user = mock(User.class);
    private final MessageChannelUnion channel = mock(MessageChannelUnion.class);
    private final QueueCallback callback = mock(QueueCallback.class);

    private AudioLoadResultHandler load() {
        doReturn(audio).when(manager).getGuildAudio(guild);
        when(audio.isConnected()).thenReturn(true);
        manager.tryAddToQueue("song", guild, channel, user, callback);
        var handler = ArgumentCaptor.forClass(AudioLoadResultHandler.class);
        verify(loader).loadItem(eq("song"), handler.capture());
        return handler.getValue();
    }

    @ParameterizedTest
    @CsvSource({"false,false", "true,false", "false,true", "true,true"})
    void terminalFailureDisconnectsOnlyWhenIdle(boolean playing, boolean loadFailure) {
        AudioLoadResultHandler handler = load();
        when(audio.isPlaying()).thenReturn(playing);
        if (loadFailure) {
            handler.loadFailed(new FriendlyException("failed", FriendlyException.Severity.COMMON, null));
        } else {
            handler.noMatches();
            verify(loader).loadItem("ytsearch:song", handler);
            verifyNoInteractions(callback);
            handler.noMatches();
            verify(loader, times(2)).loadItem(anyString(), any());
        }
        verify(audio, times(playing ? 0 : 1)).disconnect();
        verify(callback).call(loadFailure ? FAILURE_LOAD : NO_MATCHES, "song");
        verifyNoMoreInteractions(callback);
    }

    @ParameterizedTest
    @CsvSource({"false", "true"})
    void directPlaylistQueuesAllButSearchTakesOnlyFirstAcceptedResult(boolean search) {
        AudioLoadResultHandler handler = load();
        AudioTrack first = track("First");
        AudioTrack second = track("Second");
        AudioTrack third = track("Third");
        AudioPlaylist playlist = mock(AudioPlaylist.class);
        when(playlist.getTracks()).thenReturn(List.of(first, second, third));
        when(playlist.getName()).thenReturn("Playlist");
        when(audio.queue(any())).thenReturn(true);
        if (search) {
            handler.noMatches();
            when(audio.queue(first)).thenReturn(false);
        }
        handler.playlistLoaded(playlist);
        var order = inOrder(audio);
        order.verify(audio).queue(first);
        order.verify(audio).queue(second);
        verify(audio, times(search ? 0 : 1)).queue(third);
        verify(second).setUserData(new GuildTrackData(user, channel, guild));
        verify(callback).call(SUCCESS, search ? "Second" : "Playlist");
        verifyNoMoreInteractions(callback);
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void singleTrackReportsQueueOutcome(boolean accepted) {
        AudioLoadResultHandler handler = load();
        AudioTrack track = track("Title");
        when(audio.queue(track)).thenReturn(accepted);
        handler.trackLoaded(track);
        verify(track).setUserData(new GuildTrackData(user, channel, guild));
        verify(callback).call(accepted ? SUCCESS : FAILURE_QUEUE, accepted ? "Title" : "song");
    }

    @Test
    void existingConnectionIsReusedWithoutLookingUpRequestersChannel() {
        when(audio.isConnected()).thenReturn(true);
        assertTrue(manager.connectToGuild(audio, user, callback));
        verify(audio, never()).getGuild();
        verifyNoInteractions(callback);
    }

    @Test
    void missingVoiceChannelRejectsRequestBeforeLoading() {
        doReturn(audio).when(manager).getGuildAudio(guild);
        when(audio.getGuild()).thenReturn(guild);
        when(guild.getMember(user).getVoiceState()).thenReturn(null);
        manager.tryAddToQueue("song", guild, channel, user, callback);
        verify(callback).call(FAILURE_CHANNEL);
        verifyNoInteractions(loader);
    }

    private static AudioTrack track(String title) {
        AudioTrack track = mock(AudioTrack.class);
        when(track.getInfo()).thenReturn(new AudioTrackInfo(title, "Artist", 1000, title, false, "https://example.com/track"));
        return track;
    }
}
