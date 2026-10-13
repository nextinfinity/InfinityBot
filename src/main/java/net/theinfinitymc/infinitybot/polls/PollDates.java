package net.theinfinitymc.infinitybot.polls;

import java.time.*;

/** Year inference is for creation inputs only; persisted/component dates remain strict ISO. */
final class PollDates {
    private PollDates() {}

    static LocalDate resolve(String input, LocalDate onOrAfter) {
        input = input.strip();
        if (input.matches("\\d{4}-\\d{2}-\\d{2}")) return PollListener.parseDate(input);
        if (!input.matches("\\d{1,2}[-/]\\d{1,2}"))
            throw new IllegalArgumentException("Use M/D, MM-DD, or an explicit YYYY-MM-DD date.");
        try {
            String[] parts = input.split("[-/]");
            MonthDay day = MonthDay.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
            int year = onOrAfter.getYear() + (day.compareTo(MonthDay.from(onOrAfter)) < 0 ? 1 : 0);
            if (!day.isValidYear(year))
                throw new IllegalArgumentException("February 29 is not valid in the inferred year " + year + ". Enter an explicit YYYY-MM-DD date.");
            return day.atYear(year);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Invalid month/day. Use M/D, MM-DD, or YYYY-MM-DD.");
        }
    }
}
