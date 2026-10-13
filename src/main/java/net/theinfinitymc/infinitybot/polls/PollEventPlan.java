package net.theinfinitymc.infinitybot.polls;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Time exists only at event creation; availability itself stays date-only. */
public record PollEventPlan(OffsetDateTime start, OffsetDateTime end, String location) {
    public static PollEventPlan parse(LocalDate date, String time, String zone, String duration,
                                      String location, Clock clock) {
        try {
            if (!time.matches("\\d{2}:\\d{2}")) throw new IllegalArgumentException("Use 24-hour HH:mm for the start time.");
            LocalDateTime local = date.atTime(LocalTime.parse(time));
            ZoneId timezone = ZoneId.of(zone.strip());
            List<ZoneOffset> offsets = timezone.getRules().getValidOffsets(local);
            if (offsets.size() != 1)
                throw new IllegalArgumentException("That local time is skipped or ambiguous due to daylight saving time. Choose another time or use UTC.");
            OffsetDateTime start = local.atOffset(offsets.getFirst());
            int minutes = Integer.parseInt(duration.strip());
            if (minutes < 1 || minutes > 10080) throw new IllegalArgumentException("Duration must be 1–10080 minutes.");
            if (!start.toInstant().isAfter(clock.instant())) throw new IllegalArgumentException("Discord events must start in the future.");
            location = location.strip();
            if (location.isEmpty() || location.length() > 100) throw new IllegalArgumentException("Provide a voice channel ID or an external location (1–100 characters).");
            return new PollEventPlan(start, start.plusMinutes(minutes), location);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid time. Use 24-hour HH:mm.");
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Invalid timezone. Use an IANA name such as America/New_York or UTC.");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Duration must be a whole number of minutes.");
        }
    }
}
