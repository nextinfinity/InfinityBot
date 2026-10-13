package net.theinfinitymc.infinitybot.polls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

/** Debounce per poll, with at most one Discord edit in flight. Never let an old edit win. */
public final class PollUpdates implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(PollUpdates.class);
    private static final class State { boolean dirty; }
    private final Map<String, State> pending = new HashMap<>();
    private final PollStore store;
    private final ScheduledExecutorService executor;
    private final BiConsumer<Poll, Runnable> sender;
    private boolean closed;

    public PollUpdates(PollStore store, BiConsumer<Poll, Runnable> sender) {
        this(store, sender, Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "poll-message-updates");
            thread.setDaemon(true);
            return thread;
        }));
    }

    PollUpdates(PollStore store, BiConsumer<Poll, Runnable> sender, ScheduledExecutorService executor) {
        this.store = store;
        this.sender = sender;
        this.executor = executor;
    }

    public synchronized void request(String id) {
        if (closed) return;
        State state = pending.get(id);
        if (state == null) {
            state = new State();
            pending.put(id, state);
            schedule(id);
        }
        state.dirty = true;
    }

    private void schedule(String id) {
        executor.schedule(() -> flush(id), 750, TimeUnit.MILLISECONDS);
    }

    private void flush(String id) {
        synchronized (this) {
            if (closed) return;
            State state = pending.get(id);
            state.dirty = false;
        }
        try {
            Poll poll = store.get(id);
            sender.accept(poll, () -> completed(id));
        } catch (Exception e) {
            LOG.warn("Could not update poll {}", id, e);
            completed(id);
        }
    }

    private synchronized void completed(String id) {
        if (closed) return;
        State state = pending.get(id);
        if (state.dirty) schedule(id);
        else pending.remove(id);
    }

    @Override public synchronized void close() {
        closed = true;
        pending.clear();
        executor.shutdownNow();
    }
}
