package net.theinfinitymc.infinitybot.polls;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PollUiTest {
    private final LocalDate first = LocalDate.of(2026, 6, 1);
    private Poll poll(Poll.Status status, Map<String, Poll.Vote> votes) { return poll(status, votes, 25); }
    private Poll poll(Poll.Status status, Map<String, Poll.Vote> votes, int days) {
        return new Poll(UUID.randomUUID().toString(), "100", "200", "300", "400", "Game night",
            Poll.candidateDates(first, first.plusDays(days - 1), EnumSet.allOf(DayOfWeek.class)), status,
            status == Poll.Status.FINALIZED ? first : null, null, "NONE", votes);
    }

    @ParameterizedTest @ValueSource(ints = {1, 10, 11, 20, 21, 25})
    void checkboxGroupsRespectLimitsAndPreserveSelectionsAcrossBoundaries(int dateCount) {
        Set<LocalDate> selected = new HashSet<>();
        for (int i = 0; i < dateCount; i += 2) selected.add(first.plusDays(i));
        Poll poll = poll(Poll.Status.OPEN, Map.of("500", new Poll.Vote(selected)), dateCount);
        var modal = PollUi.voteModal(poll, "500");
        assertEquals((dateCount + 9) / 10, modal.getComponents().size());
        List<String> allDates = new ArrayList<>();
        Set<String> checked = new HashSet<>();
        for (int i = 0; i < modal.getComponents().size(); i++) {
            var group = modal.getComponents().get(i).asLabel().getChild().asCheckboxGroup();
            assertEquals("available" + i, group.getCustomId());
            assertTrue(group.getOptions().size() <= 10);
            assertEquals(0, group.getMinValues());
            assertEquals(group.getOptions().size(), group.getMaxValues());
            assertFalse(group.isRequired());
            for (var option : group.getOptions()) {
                allDates.add(option.getValue());
                if (option.isDefault()) checked.add(option.getValue());
            }
        }
        assertEquals(poll.dates().stream().map(LocalDate::toString).toList(), allDates);
        assertEquals(Set.copyOf(selected.stream().map(LocalDate::toString).toList()), checked);
    }

    @Test void newRespondentHasNoCheckedDatesAndCanSubmitNone() {
        var modal = PollUi.voteModal(poll(Poll.Status.OPEN, Map.of()), "500");
        for (var component : modal.getComponents()) {
            var group = component.asLabel().getChild().asCheckboxGroup();
            assertFalse(group.isRequired());
            assertEquals(0, group.getMinValues());
            assertTrue(group.getOptions().stream().noneMatch(option -> option.isDefault()));
        }
    }

    @Test void summaryIsReadableAvailabilityListWithLeadersAndNoVisiblePollId() {
        Poll poll = poll(Poll.Status.OPEN, Map.of("500", new Poll.Vote(Set.of(first)), "600", new Poll.Vote(Set.of(first))));
        var embed = PollUi.summary(poll).getEmbeds().getFirst();
        assertTrue(embed.getDescription().length() <= 4096);
        assertTrue(embed.getDescription().contains("⭐ **" + PollUi.day(first) + "** — 2 of 2 available"));
        assertTrue(embed.getDescription().contains("• " + PollUi.day(first.plusDays(1)) + " — 0 of 2 available"));
        assertFalse(embed.getDescription().contains("```"));
        assertFalse(embed.getFooter().getText().contains(poll.id()));
        assertTrue(PollUi.createModal().getComponents().size() <= 5);
        assertTrue(PollUi.eventModal(poll, first).getComponents().size() <= 5);
    }

    @Test void emptyPollShowsDatesAndWaitingStateWithoutZeroOfZeroCounts() {
        var embed = PollUi.summary(poll(Poll.Status.OPEN, Map.of())).getEmbeds().getFirst();
        assertTrue(embed.getDescription().contains("Waiting for responses"));
        assertTrue(embed.getDescription().contains(PollUi.day(first)));
        assertFalse(embed.getDescription().contains("0 of 0"));
    }

    @Test void creationHasSevenCheckedWeekdaysWithAtLeastOneRequired() {
        var days = PollUi.createModal().getComponents().get(3).asLabel().getChild().asCheckboxGroup();
        assertEquals(7, days.getOptions().size());
        assertTrue(days.getOptions().stream().allMatch(option -> option.isDefault()));
        assertEquals(Arrays.stream(DayOfWeek.values()).map(DayOfWeek::name).toList(),
            days.getOptions().stream().map(option -> option.getValue()).toList());
        assertEquals(1, days.getMinValues());
        assertEquals(7, days.getMaxValues());
        assertTrue(days.isRequired());
    }

    @Test void closedPollDisablesVotingButRetainsDetailsAndManagement() {
        for (Poll.Status status : List.of(Poll.Status.FINALIZED, Poll.Status.CANCELLED)) {
            var buttons = PollUi.summary(poll(status, Map.of())).getComponents().getFirst().asActionRow().getButtons();
            assertTrue(buttons.get(0).isDisabled());
            assertTrue(buttons.get(1).isDisabled());
            assertFalse(buttons.get(2).isDisabled());
            assertFalse(buttons.get(3).isDisabled());
        }
    }

    @Test void detailsArePaginatedAndMentionSuppressed() {
        Map<String, Poll.Vote> votes = new HashMap<>();
        for (int i = 0; i < 32; i++) votes.put("1000000000000000" + i, new Poll.Vote(Set.of(first)));
        Poll poll = poll(Poll.Status.OPEN, votes);
        var firstPage = PollUi.details(poll, first, 0);
        var lastPage = PollUi.details(poll, first, 100);
        assertTrue(firstPage.getContent().length() <= 2000);
        assertTrue(lastPage.getContent().contains("Page 3/3"));
        assertEquals(15, firstPage.getContent().split("<@", -1).length - 1);
        assertEquals(2, lastPage.getContent().split("<@", -1).length - 1);
        assertTrue(firstPage.getAllowedMentions().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> PollUi.details(poll, first.minusDays(1), 0));
    }
}
