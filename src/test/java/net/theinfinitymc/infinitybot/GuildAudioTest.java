package net.theinfinitymc.infinitybot;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.theinfinitymc.infinitybot.commands.Pause.PauseStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GuildAudioTest {
    private final Guild guild = mock(Guild.class, RETURNS_DEEP_STUBS);
    private final AudioPlayer player = mock(AudioPlayer.class);
    private final GuildAudio audio = new GuildAudio(guild, player, () -> {});
    private final AudioTrack first = mock(AudioTrack.class);
    private final AudioTrack second = mock(AudioTrack.class);

    @Test
    void startsImmediatelyWhenIdleAndOtherwiseAdvancesInFifoOrder() {
        AudioTrack current = mock(AudioTrack.class);
        when(player.startTrack(current, true)).thenReturn(true);
        assertTrue(audio.queue(current));
        assertFalse(audio.hasNext());
        when(player.getPlayingTrack()).thenReturn(current);
        assertTrue(audio.queue(first));
        assertTrue(audio.queue(second));
        when(player.startTrack(first, false)).thenReturn(true);
        when(player.startTrack(second, false)).thenReturn(true);
        assertTrue(audio.skip());
        assertTrue(audio.skip());
        var order = inOrder(player);
        order.verify(player).startTrack(first, false);
        order.verify(player).startTrack(second, false);
        assertFalse(audio.hasNext());
        verify(guild.getAudioManager(), never()).closeAudioConnection();
        assertTrue(audio.skip());
        verify(player).stopTrack();
        verify(guild.getAudioManager()).closeAudioConnection();
    }

    @Test
    void stopClearsPendingTracksAndDisconnects() {
        when(player.getPlayingTrack()).thenReturn(first);
        audio.queue(second);
        assertTrue(audio.stop());
        assertFalse(audio.hasNext());
        verify(player).stopTrack();
        verify(guild.getAudioManager()).closeAudioConnection();
    }

    @Test
    void idleControlsAreNoOpsAndPauseToggles() {
        assertFalse(audio.skip());
        assertFalse(audio.stop());
        assertEquals(PauseStatus.NO_MUSIC, audio.togglePause());
        verify(player, never()).stopTrack();
        verify(guild.getAudioManager(), never()).closeAudioConnection();
        when(player.getPlayingTrack()).thenReturn(first);
        assertEquals(PauseStatus.PAUSED, audio.togglePause());
        verify(player).setPaused(true);
        when(player.isPaused()).thenReturn(true);
        assertEquals(PauseStatus.UNPAUSED, audio.togglePause());
        verify(player).setPaused(false);
    }

    @ParameterizedTest
    @CsvSource({"FINISHED,true,false", "LOAD_FAILED,true,false", "REPLACED,false,false", "STOPPED,false,true", "CLEANUP,false,true"})
    void endReasonControlsAdvancementAndDisconnect(AudioTrackEndReason reason, boolean advance, boolean disconnect) {
        verify(player).addListener(audio);
        audio.queue(second);
        when(player.startTrack(second, false)).thenReturn(true);
        audio.onTrackEnd(player, first, reason);
        verify(player, times(advance ? 1 : 0)).startTrack(second, false);
        verify(guild.getAudioManager(), times(disconnect ? 1 : 0)).closeAudioConnection();
        assertEquals(!advance, audio.hasNext());
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void notificationFailureCannotPreventAdvancement(boolean stuck) {
        Message message = mock(Message.class);
        when(first.getUserData()).thenReturn(message);
        when(message.reply(anyString())).thenThrow(new IllegalStateException("Missing permission"));
        audio.queue(second);
        when(player.startTrack(second, false)).thenReturn(true);
        if (stuck) audio.onTrackStuck(player, first, 1000);
        else audio.onTrackEnd(player, first, AudioTrackEndReason.LOAD_FAILED);
        verify(player).startTrack(second, false);
        assertFalse(audio.hasNext());
    }

    @Test
    void failedNextTrackStopsAndDisconnects() {
        audio.queue(second);
        audio.onTrackEnd(player, first, AudioTrackEndReason.FINISHED);
        verify(player).startTrack(second, false);
        verify(player).stopTrack();
        verify(guild.getAudioManager()).closeAudioConnection();
    }
}
