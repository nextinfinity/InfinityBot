package net.theinfinitymc.infinitybot.polls;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.checkboxgroup.CheckboxGroup;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Stateless components: even old messages work after a restart. */
public final class PollUi {
    private PollUi() {}
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE, MMM d, uuuu", Locale.ENGLISH);
    public static String id(String action, String poll) { return "poll:" + action + ":" + poll; }
    public static String day(LocalDate date) { return DAY.format(date); }
    public static String safe(String value) { return MarkdownSanitizer.escape(value).replace("\n", " ").replace("\r", " "); }

    public static Modal createModal() {
        CheckboxGroup.Builder days = CheckboxGroup.create("days").setRequiredRange(1, 7);
        for (DayOfWeek day : DayOfWeek.values())
            days.addOption(day.getDisplayName(TextStyle.FULL, Locale.ENGLISH), day.name(), null, true);
        return Modal.create("poll:create", "Create availability poll").addComponents(
            input("title", "Event title", "Game night", 100, null),
            input("start", "Start date (M/D or YYYY-MM-DD)", "6/1", 10, null)
                .withDescription("Without a year: next occurrence on or after today (UTC). MM-DD also works."),
            input("end", "End date, inclusive (M/D or YYYY-MM-DD)", "6/14", 10, null)
                .withDescription("Without a year: next occurrence on or after the start date."),
            Label.of("Allowed weekdays", "Uncheck days to exclude. Maximum 25 candidate dates.", days.build())
        ).build();
    }

    public static Modal voteModal(Poll poll, String user) {
        Poll.Vote vote = poll.votes().getOrDefault(user, new Poll.Vote(Set.of()));
        Modal.Builder modal = Modal.create(id("vote", poll.id()), "Select days you're available");
        // Discord limits each checkbox group to 10 options; keep all dates in one form.
        for (int offset = 0; offset < poll.dates().size(); offset += 10) {
            List<LocalDate> dates = poll.dates().subList(offset, Math.min(offset + 10, poll.dates().size()));
            CheckboxGroup.Builder group = CheckboxGroup.create("available" + offset / 10)
                .setRequiredRange(0, dates.size()).setRequired(false);
            for (LocalDate date : dates) group.addOption(day(date), date.toString(), null, vote.yes().contains(date));
            String label = poll.dates().size() <= 10 ? "Available days" : "Available days · " + (offset + 1) + "–" + (offset + dates.size());
            modal.addComponents(Label.of(label, "Check every date you can attend. Leave all unchecked if none work.", group.build()));
        }
        return modal.build();
    }

    public static Modal eventModal(Poll poll, LocalDate date) {
        return Modal.create(id("event", poll.id()) + ":" + date, "Event · " + day(date)).addComponents(
            input("time", "Start time (24-hour HH:mm)", "19:00", 5, null),
            input("zone", "Timezone (IANA name, not abbreviation)", "America/New_York", 60, null),
            input("duration", "Duration in minutes", "180", 5, "180"),
            input("location", "Voice channel ID, or external location", "Paste voice channel ID, URL, or location", 100, null)
        ).build();
    }

    private static Label input(String id, String label, String placeholder, int max, String value) {
        TextInput.Builder builder = TextInput.create(id, TextInputStyle.SHORT).setPlaceholder(placeholder).setMaxLength(max);
        if (value != null) builder.setValue(value);
        return Label.of(label, builder.build());
    }

    public static StringSelectMenu.Builder dates(String id, Poll poll) {
        StringSelectMenu.Builder builder = StringSelectMenu.create(id);
        for (LocalDate date : poll.dates()) builder.addOption(day(date), date.toString());
        return builder;
    }

    public static MessageCreateData summary(Poll poll) {
        Set<LocalDate> leaders = poll.leadingDates();
        StringBuilder availability = new StringBuilder();
        for (LocalDate date : poll.dates()) {
            boolean leading = leaders.contains(date);
            availability.append(leading ? "⭐ **" : "• ").append(day(date)).append(leading ? "**" : "");
            if (!poll.votes().isEmpty()) availability.append(" — ").append(poll.counts(date).yes())
                .append(" of ").append(poll.votes().size()).append(" available");
            availability.append("\n");
        }
        String state = switch (poll.status()) {
            case OPEN -> poll.votes().isEmpty() ? "Open · Waiting for responses" : "Open · " + poll.votes().size() + " responses";
            case FINALIZED -> "Finalized · **" + day(poll.chosenDate()) + "** · " + poll.votes().size() + " responses";
            case CANCELLED -> "Cancelled · " + poll.votes().size() + " responses";
        };
        if (poll.eventId() != null) state += "\n[Discord event](https://discord.com/events/" + poll.guildId() + "/" + poll.eventId() + ")";
        else if (poll.eventState().equals("CREATING")) state += "\nDiscord event creation in progress.";
        else if (poll.eventState().equals("RETRY")) state += "\nDiscord event not confirmed. Creator/admin: Manage → Create / retry event.";
        EmbedBuilder embed = new EmbedBuilder().setTitle(poll.title()).setColor(poll.status() == Poll.Status.OPEN ? 0x5865F2 : 0x57F287)
            .setDescription(state + "\n\n" + availability)
            .setFooter("⭐ Most available · Only submitted responses count. Use Details to see who can attend.");
        return new MessageCreateBuilder().setEmbeds(embed.build()).setAllowedMentions(List.of()).setComponents(
            ActionRow.of(
                Button.primary(id("respond", poll.id()), "Respond / Edit").withDisabled(poll.status() != Poll.Status.OPEN),
                Button.secondary(id("withdraw", poll.id()), "Withdraw").withDisabled(poll.status() != Poll.Status.OPEN),
                Button.secondary(id("details", poll.id()), "Details"),
                Button.secondary(id("manage", poll.id()), "Manage")
            )).build();
    }

    public static MessageCreateData detailsPicker(Poll poll) {
        return message("**" + safe(poll.title()) + "** · Choose a date to see members' responses.",
            ActionRow.of(dates(id("detaildate", poll.id()), poll).build()));
    }

    public static MessageCreateData details(Poll poll, LocalDate date, int requestedPage) {
        if (!poll.dates().contains(date)) throw new IllegalArgumentException("That date is not in this poll.");
        List<Map.Entry<String, Poll.Vote>> entries = poll.votes().entrySet().stream()
            .sorted(Comparator.<Map.Entry<String, Poll.Vote>, Poll.Answer>comparing(e -> e.getValue().answer(date))
                .thenComparing(Map.Entry::getKey)).toList();
        int pages = Math.max(1, (entries.size() + 14) / 15);
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        Poll.Counts c = poll.counts(date);
        StringBuilder text = new StringBuilder("**" + safe(poll.title()) + "** · " + day(date)
            + "\nAvailable: " + c.yes() + " · Unavailable: " + c.no() + "\n");
        for (var entry : entries.subList(page * 15, Math.min(entries.size(), (page + 1) * 15)))
            text.append("\n").append(entry.getValue().answer(date) == Poll.Answer.YES ? "✅ Available" : "❌ Unavailable")
                .append(" · <@").append(entry.getKey()).append(">");
        if (entries.isEmpty()) text.append("\nNo responses yet.");
        text.append("\n\nPage ").append(page + 1).append("/").append(pages).append(" · Members who haven't responded are not listed.");
        return message(text.toString(),
            ActionRow.of(dates(id("detaildate", poll.id()), poll).setDefaultValues(date.toString()).build()),
            ActionRow.of(
                Button.secondary(id("page", poll.id()) + ":" + date + ":" + (page - 1), "Previous").withDisabled(page == 0),
                Button.secondary(id("page", poll.id()) + ":" + date + ":" + (page + 1), "Next").withDisabled(page + 1 == pages)));
    }

    public static MessageCreateData manage(Poll poll) {
        if (poll.status() == Poll.Status.OPEN)
            return message("**" + safe(poll.title()) + "** · Choose a final date, or cancel this poll.",
                ActionRow.of(dates(id("finaldate", poll.id()), poll).build()),
                ActionRow.of(Button.danger(id("cancelconfirm", poll.id()), "Cancel poll")));
        if (poll.status() == Poll.Status.FINALIZED && poll.eventId() == null)
            return message("Chosen date: **" + day(poll.chosenDate()) + "**. Voting is closed.",
                ActionRow.of(Button.primary(id("eventform", poll.id()) + ":" + poll.chosenDate(), "Create / retry event")
                    .withDisabled(poll.eventState().equals("CREATING"))));
        return message("This poll is " + poll.status().name().toLowerCase(Locale.ROOT) + "." +
            (poll.eventId() == null ? "" : "\nEvent: https://discord.com/events/" + poll.guildId() + "/" + poll.eventId()));
    }

    public static MessageCreateData confirm(Poll poll, LocalDate date) {
        if (poll.status() != Poll.Status.OPEN || !poll.dates().contains(date))
            throw new IllegalArgumentException("This poll is closed or that date is not a candidate.");
        return message("Finalize **" + safe(poll.title()) + "** for **" + day(date) + "**? This locks all responses.",
            ActionRow.of(Button.success(id("finalize", poll.id()) + ":" + date, "Finalize date only"),
                Button.primary(id("eventform", poll.id()) + ":" + date, "Finalize + create event")));
    }

    public static MessageCreateData message(String text, ActionRow... rows) {
        return new MessageCreateBuilder().setContent(text).setAllowedMentions(List.of()).setComponents(rows).build();
    }
}
