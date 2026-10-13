package net.theinfinitymc.infinitybot.polls;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PollUpdatesTest {
    @TempDir Path directory;

    @Test void coalescesBurstAndWaitsForInFlightEditBeforeSendingFreshSnapshot() {
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        Queue<Runnable> tasks = new ArrayDeque<>();
        when(scheduler.schedule(any(Runnable.class), eq(750L), eq(TimeUnit.MILLISECONDS)))
            .thenAnswer(call -> { tasks.add(call.getArgument(0)); return null; });
        List<Poll> sent = new ArrayList<>();
        Queue<Runnable> completions = new ArrayDeque<>();
        try (PollStore store = new PollStore(directory.resolve("polls.sqlite"));
             PollUpdates updates = new PollUpdates(store, (poll, done) -> { sent.add(poll); completions.add(done); }, scheduler)) {
            LocalDate date = LocalDate.of(2026, 6, 6);
            Poll poll = store.create("guild", "channel", "creator", "Title", date, date, EnumSet.allOf(DayOfWeek.class));
            updates.request(poll.id());
            updates.request(poll.id());
            assertEquals(1, tasks.size());
            tasks.remove().run();
            assertEquals(1, sent.size());
            store.vote(poll.id(), "one", Set.of(date));
            updates.request(poll.id());
            store.vote(poll.id(), "two", Set.of(date));
            updates.request(poll.id());
            assertTrue(tasks.isEmpty()); // Old snapshot still in flight.
            completions.remove().run();
            assertEquals(1, tasks.size());
            tasks.remove().run();
            assertEquals(2, sent.size());
            assertEquals(2, sent.getLast().votes().size());
            completions.remove().run();
            assertTrue(tasks.isEmpty());
            updates.request(poll.id());
            assertEquals(1, tasks.size());
        }
        verify(scheduler).shutdownNow();
    }

    @Test void senderFailureDoesNotPermanentlyBlockFutureUpdates() {
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        Queue<Runnable> tasks = new ArrayDeque<>();
        when(scheduler.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class)))
            .thenAnswer(call -> { tasks.add(call.getArgument(0)); return null; });
        try (PollStore store = new PollStore(directory.resolve("polls.sqlite"));
             PollUpdates updates = new PollUpdates(store, (poll, done) -> { throw new IllegalStateException("Delivery failed"); }, scheduler)) {
            LocalDate date = LocalDate.of(2026, 6, 6);
            String id = store.create("guild", "channel", "creator", "Title", date, date, EnumSet.allOf(DayOfWeek.class)).id();
            updates.request(id);
            tasks.remove().run();
            updates.request(id);
            assertEquals(1, tasks.size());
        }
    }
}
