package net.theinfinitymc.infinitybot.polls;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.*;

import static org.junit.jupiter.api.Assertions.*;

class PollEventPlanTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final LocalDate date = LocalDate.of(2026, 6, 6);

    @Test void convertsTimezoneAndUsesDurationAcrossMidnight() {
        PollEventPlan plan = PollEventPlan.parse(date, "23:30", "America/New_York", "180", " https://example.com ", clock);
        assertEquals(Instant.parse("2026-06-07T03:30:00Z"), plan.start().toInstant());
        assertEquals(Instant.parse("2026-06-07T06:30:00Z"), plan.end().toInstant());
        assertEquals("https://example.com", plan.location());
    }

    @ParameterizedTest @CsvSource({"2026-03-08,02:30", "2026-11-01,01:30"})
    void rejectsMissingOrAmbiguousDstTimes(String date, String time) {
        assertThrows(IllegalArgumentException.class, () -> PollEventPlan.parse(LocalDate.parse(date), time,
            "America/New_York", "60", "Somewhere", clock));
    }

    @ParameterizedTest @CsvSource({
        "24:00,UTC,60", "9:00,UTC,60", "19:00,Invalid/Zone,60",
        "19:00,UTC,0", "19:00,UTC,-1", "19:00,UTC,10081", "19:00,UTC,nope"
    })
    void rejectsInvalidEventFields(String time, String zone, String duration) {
        assertThrows(IllegalArgumentException.class, () -> PollEventPlan.parse(date, time, zone, duration, "Somewhere", clock));
    }

    @Test void rejectsPastStartAndBlankLocation() {
        assertThrows(IllegalArgumentException.class, () -> PollEventPlan.parse(LocalDate.of(2025, 12, 31), "19:00", "UTC", "60", "Somewhere", clock));
        assertThrows(IllegalArgumentException.class, () -> PollEventPlan.parse(date, "19:00", "UTC", "60", " ", clock));
    }
}
