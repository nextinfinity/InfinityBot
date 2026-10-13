package net.theinfinitymc.infinitybot.polls;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PollDatesTest {
    @ParameterizedTest @CsvSource({
        "10/12,2026-10-12", "10-13,2026-10-13", "1/2,2027-01-02",
        "06-05,2027-06-05", "2025-06-05,2025-06-05", "2028-02-29,2028-02-29"
    })
    void resolvesShorthandOnOrAfterReferenceAndPreservesExplicitYear(String input, String expected) {
        assertEquals(LocalDate.parse(expected), PollDates.resolve(input, LocalDate.of(2026, 10, 12)));
    }

    @Test void endIsRelativeToStartAcrossDecemberAndWhenStartIsAlreadyNextYear() {
        LocalDate today = LocalDate.of(2026, 10, 12);
        LocalDate start = PollDates.resolve("12/28", today);
        assertEquals(LocalDate.of(2027, 1, 3), PollDates.resolve("1/3", start));
        start = PollDates.resolve("1/5", today);
        assertEquals(LocalDate.of(2027, 1, 10), PollDates.resolve("1/10", start));
        assertEquals(start, PollDates.resolve("1/5", start));
    }

    @ParameterizedTest @CsvSource({"2/30", "13/1", "0/1", "6/0", "06/05/2026", "2026-02-29", "2/29"})
    void rejectsInvalidDatesWithoutSilentlyClampingLeapDay(String input) {
        assertThrows(IllegalArgumentException.class, () -> PollDates.resolve(input, LocalDate.of(2026, 1, 1)));
    }

    @Test void leapDayIsAcceptedWhenInferredYearIsLeapYear() {
        assertEquals(LocalDate.of(2028, 2, 29), PollDates.resolve("2/29", LocalDate.of(2027, 12, 1)));
    }
}
