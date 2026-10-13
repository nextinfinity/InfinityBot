package net.theinfinitymc.infinitybot.polls;

import java.nio.file.*;
import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

/** One synchronized connection; each mutation is committed before Discord is updated. */
public final class PollStore implements AutoCloseable {
    private final Connection db;

    public PollStore(Path path) {
        try {
            Path parent = path.toAbsolutePath().getParent();
            Files.createDirectories(parent);
            db = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
            try (Statement s = db.createStatement()) {
                s.execute("PRAGMA foreign_keys = ON");
                s.execute("PRAGMA busy_timeout = 5000");
                s.execute("PRAGMA journal_mode = WAL");
                s.execute("""
                    CREATE TABLE IF NOT EXISTS polls (
                      id TEXT PRIMARY KEY, guild_id TEXT NOT NULL, channel_id TEXT NOT NULL,
                      message_id TEXT, creator_id TEXT NOT NULL, title TEXT NOT NULL, dates TEXT NOT NULL,
                      status TEXT NOT NULL DEFAULT 'OPEN', chosen_date TEXT, event_id TEXT,
                      event_state TEXT NOT NULL DEFAULT 'NONE')
                    """);
                s.execute("""
                    CREATE TABLE IF NOT EXISTS poll_votes (
                      poll_id TEXT NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
                      user_id TEXT NOT NULL, yes_dates TEXT NOT NULL,
                      PRIMARY KEY (poll_id, user_id))
                    """);
                // A crash may have happened after Discord accepted creation. Reconcile before retrying.
                s.execute("UPDATE polls SET event_state = 'RETRY' WHERE event_state = 'CREATING'");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Cannot open poll database at " + path, e);
        }
    }

    public synchronized Poll create(String guild, String channel, String creator, String title,
                                    LocalDate start, LocalDate end, Set<DayOfWeek> allowedDays) {
        title = title.strip();
        if (title.isEmpty() || title.length() > 100)
            throw new IllegalArgumentException("Title must contain 1–100 characters.");
        List<LocalDate> dates = Poll.candidateDates(start, end, allowedDays);
        String id = UUID.randomUUID().toString();
        update("INSERT INTO polls (id,guild_id,channel_id,creator_id,title,dates) VALUES (?,?,?,?,?,?)",
            id, guild, channel, creator, title, encode(dates));
        return get(id);
    }

    public synchronized Poll get(String id) {
        try (PreparedStatement s = db.prepareStatement("SELECT * FROM polls WHERE id = ?")) {
            s.setString(1, id);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) throw new IllegalArgumentException("This poll is no longer available.");
                Map<String, Poll.Vote> votes = new HashMap<>();
                try (PreparedStatement v = db.prepareStatement("SELECT * FROM poll_votes WHERE poll_id = ?")) {
                    v.setString(1, id);
                    try (ResultSet rows = v.executeQuery()) {
                        while (rows.next()) votes.put(rows.getString("user_id"), new Poll.Vote(
                            new HashSet<>(decode(rows.getString("yes_dates")))));
                    }
                }
                String chosen = r.getString("chosen_date");
                return new Poll(id, r.getString("guild_id"), r.getString("channel_id"), r.getString("message_id"),
                    r.getString("creator_id"), r.getString("title"), decode(r.getString("dates")),
                    Poll.Status.valueOf(r.getString("status")), chosen == null ? null : LocalDate.parse(chosen),
                    r.getString("event_id"), r.getString("event_state"), votes);
            }
        } catch (SQLException e) { throw failure(e); }
    }

    public synchronized List<String> publishedIds() {
        try (Statement s = db.createStatement(); ResultSet r = s.executeQuery("SELECT id FROM polls WHERE message_id IS NOT NULL")) {
            List<String> ids = new ArrayList<>();
            while (r.next()) ids.add(r.getString(1));
            return ids;
        } catch (SQLException e) { throw failure(e); }
    }

    public synchronized void bindMessage(String id, String messageId) {
        update("UPDATE polls SET message_id = ? WHERE id = ?", messageId, id);
    }

    public synchronized void deleteUnpublished(String id) {
        update("DELETE FROM polls WHERE id = ? AND message_id IS NULL", id);
    }

    public synchronized void vote(String id, String user, Set<LocalDate> yes) {
        Poll poll = requireOpen(id);
        Poll.Vote vote = new Poll.Vote(yes);
        if (!poll.dates().containsAll(vote.yes()))
            throw new IllegalArgumentException("Your response contains dates outside this poll.");
        update("""
            INSERT INTO poll_votes (poll_id,user_id,yes_dates) VALUES (?,?,?)
            ON CONFLICT(poll_id,user_id) DO UPDATE SET yes_dates=excluded.yes_dates
            """, id, user, encode(vote.yes()));
    }

    public synchronized void withdraw(String id, String user) {
        requireOpen(id);
        update("DELETE FROM poll_votes WHERE poll_id = ? AND user_id = ?", id, user);
    }

    public synchronized void finalizePoll(String id, LocalDate date) {
        Poll poll = get(id);
        // A second confirmation of the same date can proceed to optional event creation.
        if (poll.status() == Poll.Status.FINALIZED && date.equals(poll.chosenDate())) return;
        requireOpen(id);
        if (!poll.dates().contains(date)) throw new IllegalArgumentException("Choose one of this poll's candidate dates.");
        update("UPDATE polls SET status='FINALIZED', chosen_date=? WHERE id=?", date.toString(), id);
    }

    public synchronized void cancel(String id) {
        requireOpen(id);
        update("UPDATE polls SET status='CANCELLED' WHERE id=?", id);
    }

    public synchronized boolean claimEvent(String id) {
        Poll poll = get(id);
        if (poll.status() != Poll.Status.FINALIZED) throw new IllegalArgumentException("Finalize the poll before creating an event.");
        if (poll.eventId() != null || poll.eventState().equals("CREATING")) return false;
        update("UPDATE polls SET event_state='CREATING' WHERE id=?", id);
        return true;
    }

    public synchronized void eventCreated(String id, String eventId) {
        update("UPDATE polls SET event_id=?, event_state='CREATED' WHERE id=?", eventId, id);
    }

    public synchronized void eventFailed(String id) {
        update("UPDATE polls SET event_state='RETRY' WHERE id=? AND event_id IS NULL", id);
    }

    private Poll requireOpen(String id) {
        Poll poll = get(id);
        if (poll.status() != Poll.Status.OPEN) throw new IllegalArgumentException("This poll is closed; responses can no longer change.");
        return poll;
    }

    private void update(String sql, String... values) {
        try (PreparedStatement s = db.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) s.setString(i + 1, values[i]);
            s.executeUpdate();
        } catch (SQLException e) { throw failure(e); }
    }

    private static String encode(Collection<LocalDate> dates) {
        return String.join(",", dates.stream().sorted().map(LocalDate::toString).toList());
    }
    private static List<LocalDate> decode(String dates) {
        return dates.isEmpty() ? List.of() : Arrays.stream(dates.split(",")).map(LocalDate::parse).toList();
    }
    private static IllegalStateException failure(SQLException e) {
        return new IllegalStateException("Poll storage failed. Please try again or contact the bot operator.", e);
    }
    @Override public synchronized void close() {
        try { db.close(); } catch (SQLException e) { throw failure(e); }
    }
}
