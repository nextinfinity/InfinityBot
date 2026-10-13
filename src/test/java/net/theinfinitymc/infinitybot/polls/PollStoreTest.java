package net.theinfinitymc.infinitybot.polls;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class PollStoreTest {
    @TempDir Path directory;
    private final LocalDate first = LocalDate.of(2026, 6, 5);
    private final LocalDate second = first.plusDays(1);
    private Poll create(PollStore store) {
        return store.create("guild", "channel", "creator", "Game night", first, first.plusDays(2), EnumSet.allOf(DayOfWeek.class));
    }

    @Test void persistsResponsesMessagesFinalizationAndEventAcrossReopen() {
        Path path = directory.resolve("nested/polls.sqlite");
        String id;
        try (PollStore store = new PollStore(path)) {
            Poll poll = create(store);
            id = poll.id();
            store.bindMessage(id, "message");
            store.vote(id, "member", Set.of(first, second));
            store.vote(id, "none", Set.of());
            assertEquals(new Poll.Counts(1, 1), store.get(id).counts(first));
            assertEquals(new Poll.Counts(1, 1), store.get(id).counts(second));
            assertEquals(new Poll.Counts(0, 2), store.get(id).counts(first.plusDays(2)));
            store.finalizePoll(id, second);
            assertTrue(store.claimEvent(id));
            store.eventCreated(id, "event");
        }
        try (PollStore store = new PollStore(path)) {
            Poll poll = store.get(id);
            assertEquals(2, poll.votes().size());
            assertEquals(Poll.Status.FINALIZED, poll.status());
            assertEquals(second, poll.chosenDate());
            assertEquals("event", poll.eventId());
            assertEquals("CREATED", poll.eventState());
            assertEquals("https://discord.com/channels/guild/channel/message", poll.messageUrl());
            assertEquals(Set.of(id), Set.copyOf(store.publishedIds()));
            assertFalse(store.claimEvent(id));
        }
    }

    @Test void editsReplaceRatherThanAccumulateAndWithdrawRemovesResponse() {
        try (PollStore store = new PollStore(directory.resolve("polls.sqlite"))) {
            String id = create(store).id();
            store.vote(id, "member", Set.of(first, second));
            store.vote(id, "member", Set.of(second));
            Poll poll = store.get(id);
            assertEquals(1, poll.votes().size());
            assertEquals(new Poll.Counts(0, 1), poll.counts(first));
            assertEquals(new Poll.Counts(1, 0), poll.counts(second));
            store.withdraw(id, "member");
            assertTrue(store.get(id).votes().isEmpty());
        }
    }

    @Test void invalidVoteDoesNotOverwriteExistingResponse() {
        try (PollStore store = new PollStore(directory.resolve("polls.sqlite"))) {
            String id = create(store).id();
            store.vote(id, "member", Set.of(first));
            assertThrows(IllegalArgumentException.class, () -> store.vote(id, "member", Set.of(first.minusDays(1))));
            assertEquals(Set.of(first), store.get(id).votes().get("member").yes());
        }
    }

    @Test void closedPollRejectsVotesWithdrawalAndDifferentFinalDate() {
        try (PollStore store = new PollStore(directory.resolve("polls.sqlite"))) {
            String id = create(store).id();
            assertThrows(IllegalArgumentException.class, () -> store.finalizePoll(id, first.minusDays(1)));
            store.vote(id, "member", Set.of(first));
            store.finalizePoll(id, first);
            store.finalizePoll(id, first); // Same-date finalization permits optional event retries.
            assertThrows(IllegalArgumentException.class, () -> store.vote(id, "member", Set.of()));
            assertThrows(IllegalArgumentException.class, () -> store.withdraw(id, "member"));
            assertThrows(IllegalArgumentException.class, () -> store.finalizePoll(id, second));
            assertThrows(IllegalArgumentException.class, () -> store.cancel(id));
            assertEquals(Set.of(first), store.get(id).votes().get("member").yes());
        }
    }

    @Test void cancellationDoesNotPickADateOrAllowEventCreation() {
        try (PollStore store = new PollStore(directory.resolve("polls.sqlite"))) {
            String id = create(store).id();
            store.cancel(id);
            Poll poll = store.get(id);
            assertEquals(Poll.Status.CANCELLED, poll.status());
            assertNull(poll.chosenDate());
            assertThrows(IllegalArgumentException.class, () -> store.finalizePoll(id, first));
            assertThrows(IllegalArgumentException.class, () -> store.vote(id, "member", Set.of()));
            assertThrows(IllegalArgumentException.class, () -> store.claimEvent(id));
        }
    }

    @Test void eventClaimIsExclusiveAndFailedOrInterruptedCreationCanBeRetried() {
        Path path = directory.resolve("polls.sqlite");
        String id;
        try (PollStore store = new PollStore(path)) {
            id = create(store).id();
            String pollId = id;
            assertThrows(IllegalArgumentException.class, () -> store.claimEvent(pollId));
            store.finalizePoll(id, first);
            assertTrue(store.claimEvent(id));
            assertFalse(store.claimEvent(id));
            store.eventFailed(id);
            assertTrue(store.claimEvent(id));
        }
        try (PollStore store = new PollStore(path)) {
            assertEquals("RETRY", store.get(id).eventState());
            assertTrue(store.claimEvent(id));
        }
    }

    @Test void pollsHaveIndependentVotesAndPublishingFailureCleanupIsSafe() {
        try (PollStore store = new PollStore(directory.resolve("polls.sqlite"))) {
            String one = create(store).id(), two = create(store).id();
            store.vote(one, "member", Set.of(first));
            assertTrue(store.get(two).votes().isEmpty());
            store.bindMessage(one, "message");
            store.deleteUnpublished(one);
            assertEquals(1, store.get(one).votes().size());
            store.deleteUnpublished(two);
            assertThrows(IllegalArgumentException.class, () -> store.get(two));
        }
    }

    @Test void concurrentVotersDoNotLoseEachOthersResponses() throws Exception {
        try (PollStore store = new PollStore(directory.resolve("polls.sqlite"));
             ExecutorService threads = Executors.newFixedThreadPool(4)) {
            String id = create(store).id();
            var futures = new java.util.ArrayList<Future<?>>();
            for (int i = 0; i < 20; i++) {
                String user = Integer.toString(i);
                futures.add(threads.submit(() -> store.vote(id, user, Set.of(first))));
            }
            for (var future : futures) future.get(5, TimeUnit.SECONDS);
            assertEquals(new Poll.Counts(20, 0), store.get(id).counts(first));
        }
    }
}
