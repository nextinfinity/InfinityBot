package net.theinfinitymc.infinitybot.polls;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

/** Immutable snapshots: unanswered members are not votes, and omitted dates in a saved vote are No. */
public record Poll(String id, String guildId, String channelId, String messageId, String creatorId,
                   String title, List<LocalDate> dates, Status status, LocalDate chosenDate,
                   String eventId, String eventState, Map<String, Vote> votes) {
    public enum Status { OPEN, FINALIZED, CANCELLED }
    public enum Answer { YES, NO }
    public record Vote(Set<LocalDate> yes) {
        public Vote { yes = Set.copyOf(yes); }
        public Answer answer(LocalDate date) { return yes.contains(date) ? Answer.YES : Answer.NO; }
    }
    public record Counts(int yes, int no) {}

    public Poll {
        dates = List.copyOf(dates);
        votes = Map.copyOf(votes);
    }

    public static List<LocalDate> candidateDates(LocalDate start, LocalDate end, Set<DayOfWeek> allowedDays) {
        Set<DayOfWeek> days = Set.copyOf(allowedDays);
        if (days.isEmpty()) throw new IllegalArgumentException("Select at least one allowed weekday.");
        if (end.isBefore(start)) throw new IllegalArgumentException("End date must be on or after start date.");
        // Bound iteration even for accidentally entered years or sparse weekday selections.
        if (end.toEpochDay() - start.toEpochDay() > 90)
            throw new IllegalArgumentException("The date range must be at most 90 days.");
        List<LocalDate> dates = start.datesUntil(end.plusDays(1))
            .filter(d -> days.contains(d.getDayOfWeek()))
            .toList();
        if (dates.isEmpty()) throw new IllegalArgumentException("That range contains no selectable dates.");
        if (dates.size() > 25) throw new IllegalArgumentException("A poll can contain at most 25 candidate dates. Shorten the range or select fewer weekdays.");
        return dates;
    }

    public Counts counts(LocalDate date) {
        int yes = 0;
        for (Vote vote : votes.values()) if (vote.answer(date) == Answer.YES) yes++;
        return new Counts(yes, votes.size() - yes);
    }

    public Set<LocalDate> leadingDates() {
        if (votes.isEmpty()) return Set.of();
        int best = dates.stream().mapToInt(date -> counts(date).yes()).max().orElseThrow();
        if (best == 0) return Set.of();
        Set<LocalDate> result = new HashSet<>();
        for (LocalDate date : dates) if (counts(date).yes() == best) result.add(date);
        return Set.copyOf(result);
    }

    public boolean canManage(String userId, boolean administrator) {
        return administrator || creatorId.equals(userId);
    }

    public String messageUrl() {
        return messageId == null ? "" : "https://discord.com/channels/" + guildId + "/" + channelId + "/" + messageId;
    }
}
