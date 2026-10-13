package net.theinfinitymc.infinitybot.polls;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PollTest {
    private final LocalDate friday = LocalDate.of(2026, 6, 5);

    @Test void inclusiveDateRangeAndWeekdayFilter() {
        Set<DayOfWeek> all = EnumSet.allOf(DayOfWeek.class);
        Set<DayOfWeek> weekends = EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        assertEquals(List.of(friday), Poll.candidateDates(friday, friday, all));
        assertEquals(List.of(friday.plusDays(1), friday.plusDays(2)), Poll.candidateDates(friday, friday.plusDays(3), weekends));
        assertEquals(List.of(friday, friday.plusDays(3)), Poll.candidateDates(friday, friday.plusDays(4),
            EnumSet.of(DayOfWeek.FRIDAY, DayOfWeek.MONDAY)));
        assertEquals(25, Poll.candidateDates(friday, friday.plusDays(24), all).size());
        assertThrows(IllegalArgumentException.class, () -> Poll.candidateDates(friday, friday.plusDays(25), all));
        assertThrows(IllegalArgumentException.class, () -> Poll.candidateDates(friday, friday, weekends));
        assertThrows(IllegalArgumentException.class, () -> Poll.candidateDates(friday, friday, Set.of()));
        assertThrows(IllegalArgumentException.class, () -> Poll.candidateDates(friday, friday.minusDays(1), all));
        assertThrows(IllegalArgumentException.class, () -> Poll.candidateDates(friday, friday.plusYears(1), weekends));
    }

    @Test void leadersPreferMostAvailablePreserveTiesAndExcludeAllUnavailable() {
        LocalDate saturday = friday.plusDays(1);
        var one = new Poll.Vote(Set.of(friday, saturday));
        var two = new Poll.Vote(Set.of(friday));
        assertEquals(Set.of(friday), poll(Map.of("one", one, "two", two)).leadingDates());
        assertEquals(Set.of(friday, saturday), poll(Map.of("one", one)).leadingDates());
        assertEquals(Set.of(), poll(Map.of()).leadingDates());
        assertEquals(Set.of(), poll(Map.of("none", new Poll.Vote(Set.of()))).leadingDates());
    }

    @Test void creatorOrAdministratorCanManage() {
        Poll poll = poll(Map.of());
        assertTrue(poll.canManage("creator", false));
        assertTrue(poll.canManage("another", true));
        assertFalse(poll.canManage("another", false));
    }

    @ParameterizedTest @CsvSource({"2026-02-30", "2026-2-01", "06/05/2026", "2026-13-01"})
    void datesAreStrictIso(String input) {
        assertThrows(IllegalArgumentException.class, () -> PollListener.parseDate(input));
    }

    private Poll poll(Map<String, Poll.Vote> votes) {
        return new Poll("id", "guild", "channel", "message", "creator", "Title",
            List.of(friday, friday.plusDays(1), friday.plusDays(2)), Poll.Status.OPEN, null, null, "NONE", votes);
    }
}
