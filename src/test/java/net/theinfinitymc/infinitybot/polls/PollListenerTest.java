package net.theinfinitymc.infinitybot.polls;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.ScheduledEventAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked") // Mockito's erased RestAction/Consumer types.
class PollListenerTest {
    @TempDir Path directory;
    private final Guild guild = mock(Guild.class, RETURNS_DEEP_STUBS);
    private final Member member = mock(Member.class);
    private final TextChannel channel = mock(TextChannel.class);
    private final LocalDate date = LocalDate.of(2090, 6, 6);

    private Poll create(PollStore store) {
        when(guild.getId()).thenReturn("100");
        when(guild.getTextChannelById("200")).thenReturn(channel);
        when(channel.getId()).thenReturn("200");
        when(member.hasPermission(channel, Permission.VIEW_CHANNEL)).thenReturn(true);
        return store.create("100", "200", "400", "Game night", date, date.plusDays(1), EnumSet.allOf(DayOfWeek.class));
    }
    private void identity(Interaction event, String user) {
        when(event.isFromGuild()).thenReturn(true);
        when(event.getGuild()).thenReturn(guild);
        when(event.getMember()).thenReturn(member);
        when(event.getUser().getId()).thenReturn(user);
        if (event instanceof IReplyCallback reply) {
            WebhookMessageEditAction<Message> edit = mock(WebhookMessageEditAction.class, RETURNS_SELF);
            when(reply.getHook().editOriginal(anyString())).thenReturn(edit);
        }
    }
    private ModalInteractionEvent eventForm(Poll poll) {
        ModalInteractionEvent event = mock(ModalInteractionEvent.class, RETURNS_DEEP_STUBS);
        identity(event, "400");
        when(event.isAcknowledged()).thenReturn(true);
        when(event.getModalId()).thenReturn(PollUi.id("event", poll.id()) + ":" + date);
        input(event, "time", "19:00");
        input(event, "zone", "UTC");
        input(event, "duration", "180");
        input(event, "location", "Somewhere");
        return event;
    }
    private void input(ModalInteractionEvent event, String key, String value) {
        ModalMapping mapping = mock(ModalMapping.class);
        when(mapping.getAsString()).thenReturn(value);
        when(event.getValue(key)).thenReturn(mapping);
    }
    private void eventPermission() {
        when(guild.getSelfMember().hasPermission(Permission.CREATE_SCHEDULED_EVENTS)).thenReturn(true);
    }
    private RestAction<List<ScheduledEvent>> eventList(List<ScheduledEvent> events) {
        RestAction<List<ScheduledEvent>> action = mock(RestAction.class);
        when(guild.retrieveScheduledEvents()).thenReturn(action);
        doAnswer(call -> { ((Consumer<List<ScheduledEvent>>) call.getArgument(0)).accept(events); return null; })
            .when(action).queue(any(), any());
        return action;
    }

    @Test void uncheckedDatesMeanUnavailableAndClosedPollRejectsStaleSubmission() {
        try (PollStore store = new PollStore(directory.resolve("polls.sqlite")); PollListener listener = new PollListener(store)) {
            Poll poll = create(store);
            ModalInteractionEvent event = mock(ModalInteractionEvent.class, RETURNS_DEEP_STUBS);
            identity(event, "500");
            when(event.isAcknowledged()).thenReturn(true);
            when(event.getModalId()).thenReturn(PollUi.id("vote", poll.id()));
            when(event.getValue("available0")).thenReturn(null);
            listener.onModalInteraction(event);
            assertEquals(new Poll.Counts(0, 1), store.get(poll.id()).counts(date));
            store.finalizePoll(poll.id(), date);
            ModalMapping selection = mock(ModalMapping.class);
            when(selection.getAsStringList()).thenReturn(List.of(date.toString()));
            when(event.getValue("available0")).thenReturn(selection);
            listener.onModalInteraction(event);
            assertEquals(new Poll.Counts(0, 1), store.get(poll.id()).counts(date));
        }
    }

    @Test void votesAggregateAllCheckboxGroupsAndEditsReplaceTheWholeResponse() {
        try (PollStore store = new PollStore(directory.resolve("polls.sqlite")); PollListener listener = new PollListener(store)) {
            create(store); // Configure guild and channel access.
            Poll poll = store.create("100", "200", "400", "Long poll", date, date.plusDays(24), EnumSet.allOf(DayOfWeek.class));
            ModalInteractionEvent event = mock(ModalInteractionEvent.class, RETURNS_DEEP_STUBS);
            identity(event, "500");
            when(event.isAcknowledged()).thenReturn(true);
            when(event.getModalId()).thenReturn(PollUi.id("vote", poll.id()));
            Set<LocalDate> selected = Set.of(date, date.plusDays(10), date.plusDays(24));
            for (int group = 0; group < 3; group++) {
                ModalMapping mapping = mock(ModalMapping.class);
                LocalDate selection = group == 0 ? date : group == 1 ? date.plusDays(10) : date.plusDays(24);
                when(mapping.getAsStringList()).thenReturn(List.of(selection.toString()));
                when(event.getValue("available" + group)).thenReturn(mapping);
            }
            listener.onModalInteraction(event);
            assertEquals(selected, store.get(poll.id()).votes().get("500").yes());
            when(event.getValue("available1")).thenReturn(null);
            when(event.getValue("available2")).thenReturn(null);
            listener.onModalInteraction(event);
            assertEquals(Set.of(date), store.get(poll.id()).votes().get("500").yes());
        }
    }

    @Test void viewingRequiresSameGuildAndOriginalChannelAccess() {
        try (PollStore store = new PollStore(directory.resolve("polls.sqlite")); PollListener listener = new PollListener(store)) {
            Poll poll = create(store);
            ButtonInteractionEvent event = mock(ButtonInteractionEvent.class, RETURNS_DEEP_STUBS);
            identity(event, "500");
            assertEquals(poll.id(), listener.accessible(event, poll.id()).id());
            when(member.hasPermission(channel, Permission.VIEW_CHANNEL)).thenReturn(false);
            assertThrows(IllegalArgumentException.class, () -> listener.accessible(event, poll.id()));
            when(member.hasPermission(channel, Permission.VIEW_CHANNEL)).thenReturn(true);
            when(guild.getId()).thenReturn("other");
            assertThrows(IllegalArgumentException.class, () -> listener.accessible(event, poll.id()));
        }
    }

    @Test void unauthorizedManagementButtonAndModalCannotClosePoll() {
        try (PollStore store = new PollStore(directory.resolve("polls.sqlite")); PollListener listener = new PollListener(store)) {
            Poll poll = create(store);
            ButtonInteractionEvent button = mock(ButtonInteractionEvent.class, RETURNS_DEEP_STUBS);
            identity(button, "500");
            when(button.getComponentId()).thenReturn(PollUi.id("finalize", poll.id()) + ":" + date);
            listener.onButtonInteraction(button);
            assertEquals(Poll.Status.OPEN, store.get(poll.id()).status());
            ModalInteractionEvent event = eventForm(poll);
            when(event.getUser().getId()).thenReturn("500");
            listener.onModalInteraction(event);
            assertEquals(Poll.Status.OPEN, store.get(poll.id()).status());
            when(member.hasPermission(Permission.ADMINISTRATOR)).thenReturn(true);
            listener.onButtonInteraction(button);
            assertEquals(Poll.Status.FINALIZED, store.get(poll.id()).status());
        }
    }

    @Test void respondOpensModalWithoutDeferring() {
        try (PollStore store = new PollStore(directory.resolve("polls.sqlite")); PollListener listener = new PollListener(store)) {
            Poll poll = create(store);
            ButtonInteractionEvent event = mock(ButtonInteractionEvent.class, RETURNS_DEEP_STUBS);
            identity(event, "500");
            when(event.getComponentId()).thenReturn(PollUi.id("respond", poll.id()));
            listener.onButtonInteraction(event);
            verify(event).replyModal(any(Modal.class));
            verify(event, never()).deferReply(anyBoolean());
        }
    }

    @Test void missingEventPermissionsStillFinalizesDateWithoutCallingDiscord() {
        try (PollStore store = new PollStore(directory.resolve("polls.sqlite")); PollListener listener = new PollListener(store)) {
            Poll poll = create(store);
            listener.onModalInteraction(eventForm(poll));
            assertEquals(Poll.Status.FINALIZED, store.get(poll.id()).status());
            assertEquals(date, store.get(poll.id()).chosenDate());
            verify(guild, never()).retrieveScheduledEvents();
        }
    }

    @Test void externalEventCreationIsRecordedAndRepeatSubmissionDoesNotDuplicateIt() {
        try (PollStore store = new PollStore(directory.resolve("polls.sqlite")); PollListener listener = new PollListener(store)) {
            Poll poll = create(store);
            eventPermission();
            eventList(List.of());
            ScheduledEventAction action = mock(ScheduledEventAction.class, RETURNS_SELF);
            when(guild.createScheduledEvent(eq("Game night"), eq("Somewhere"), any(), any())).thenReturn(action);
            ScheduledEvent created = mock(ScheduledEvent.class);
            when(created.getId()).thenReturn("700");
            doAnswer(call -> { ((Consumer<ScheduledEvent>) call.getArgument(0)).accept(created); return null; })
                .when(action).queue(any(), any());
            ModalInteractionEvent event = eventForm(poll);
            listener.onModalInteraction(event);
            assertEquals("700", store.get(poll.id()).eventId());
            listener.onModalInteraction(event);
            verify(guild, times(1)).createScheduledEvent(eq("Game night"), eq("Somewhere"), any(), any());
            verify(action).setDescription(contains("[InfinityBot poll: " + poll.id() + "]"));
        }
    }

    @Test void retryReconcilesPreviouslyCreatedEventBeforeCreatingAnother() {
        try (PollStore store = new PollStore(directory.resolve("polls.sqlite")); PollListener listener = new PollListener(store)) {
            Poll poll = create(store);
            store.finalizePoll(poll.id(), date);
            eventPermission();
            ModalInteractionEvent event = eventForm(poll);
            when(event.getJDA().getSelfUser().getId()).thenReturn("800");
            ScheduledEvent existing = mock(ScheduledEvent.class);
            when(existing.getId()).thenReturn("700");
            when(existing.getCreatorId()).thenReturn("800");
            when(existing.getDescription()).thenReturn("Previously created\n[InfinityBot poll: " + poll.id() + "]");
            eventList(List.of(existing));
            listener.onModalInteraction(event);
            assertEquals("700", store.get(poll.id()).eventId());
            verify(guild, never()).createScheduledEvent(anyString(), anyString(), any(), any());
        }
    }

    @Test void eventApiFailureLeavesChosenDateIntactAndReleasesClaimForRetry() {
        try (PollStore store = new PollStore(directory.resolve("polls.sqlite")); PollListener listener = new PollListener(store)) {
            Poll poll = create(store);
            eventPermission();
            eventList(List.of());
            ScheduledEventAction action = mock(ScheduledEventAction.class, RETURNS_SELF);
            when(guild.createScheduledEvent(anyString(), anyString(), any(), any())).thenReturn(action);
            doAnswer(call -> { ((Consumer<Throwable>) call.getArgument(1)).accept(new IllegalStateException("Discord unavailable")); return null; })
                .when(action).queue(any(), any());
            listener.onModalInteraction(eventForm(poll));
            assertEquals(date, store.get(poll.id()).chosenDate());
            assertEquals("RETRY", store.get(poll.id()).eventState());
            assertTrue(store.claimEvent(poll.id()));
        }
    }

    @Test void voiceEventRequiresChannelAccessAndConnect() {
        try (PollStore store = new PollStore(directory.resolve("polls.sqlite")); PollListener listener = new PollListener(store)) {
            Poll poll = create(store);
            eventPermission();
            VoiceChannel voice = mock(VoiceChannel.class);
            when(guild.getVoiceChannelById("600")).thenReturn(voice);
            ModalInteractionEvent event = eventForm(poll);
            input(event, "location", "600");
            listener.onModalInteraction(event);
            assertEquals(Poll.Status.FINALIZED, store.get(poll.id()).status());
            verify(guild, never()).retrieveScheduledEvents();
            when(guild.getSelfMember().hasPermission(voice, Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT)).thenReturn(true);
            eventList(List.of());
            ScheduledEventAction action = mock(ScheduledEventAction.class, RETURNS_SELF);
            when(guild.createScheduledEvent(eq("Game night"), eq(voice), any())).thenReturn(action);
            listener.onModalInteraction(event);
            verify(guild).createScheduledEvent(eq("Game night"), eq(voice), any());
            verify(action).setEndTime(any());
        }
    }

    @Test void delegatedCreatorCanOpenCreationModalWithoutDeferring() {
        try (PollStore store = new PollStore(directory.resolve("polls.sqlite")); PollListener listener = new PollListener(store)) {
            create(store);
            SlashCommandInteractionEvent event = mock(SlashCommandInteractionEvent.class, RETURNS_DEEP_STUBS);
            identity(event, "500"); // Not an admin: creation is governed by Discord command permissions.
            when(event.getName()).thenReturn("poll");
            when(event.getChannelType()).thenReturn(ChannelType.TEXT);
            when(event.getChannelId()).thenReturn("200");
            when(guild.getSelfMember().hasPermission(channel, Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND,
                Permission.MESSAGE_EMBED_LINKS)).thenReturn(true);
            listener.onSlashCommandInteraction(event);
            verify(event).replyModal(any(Modal.class));
            verify(event, never()).deferReply(anyBoolean());
        }
    }

    @ParameterizedTest @ValueSource(strings = {"explicit", "inferred"})
    void creationPublishesExactlyOneMessageAndBindsItToPoll(String format) {
        try (PollStore store = new PollStore(directory.resolve("polls.sqlite")); PollListener listener = new PollListener(store)) {
            create(store);
            ModalInteractionEvent event = mock(ModalInteractionEvent.class, RETURNS_DEEP_STUBS);
            identity(event, "500");
            when(event.isAcknowledged()).thenReturn(true);
            when(event.getModalId()).thenReturn("poll:create");
            when(event.getChannelType()).thenReturn(ChannelType.TEXT);
            when(event.getChannelId()).thenReturn("200");
            when(guild.getSelfMember().hasPermission(channel, Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND,
                Permission.MESSAGE_EMBED_LINKS)).thenReturn(true);
            input(event, "title", "Weekend plan");
            LocalDate start = format.equals("explicit") ? date : LocalDate.now(Clock.systemUTC()).plusDays(2);
            LocalDate end = start.plusDays(10);
            input(event, "start", format.equals("explicit") ? start.toString() : start.getMonthValue() + "/" + start.getDayOfMonth());
            input(event, "end", format.equals("explicit") ? end.toString() : end.getMonthValue() + "/" + end.getDayOfMonth());
            ModalMapping days = mock(ModalMapping.class);
            when(days.getAsStringList()).thenReturn(List.of("SATURDAY", "SUNDAY"));
            when(event.getValue("days")).thenReturn(days);
            MessageCreateAction action = mock(MessageCreateAction.class, RETURNS_SELF);
            when(channel.sendMessage(any(MessageCreateData.class))).thenReturn(action);
            Message message = mock(Message.class);
            when(message.getId()).thenReturn("300");
            doAnswer(call -> { ((Consumer<Message>) call.getArgument(0)).accept(message); return null; })
                .when(action).queue(any(), any());
            listener.onModalInteraction(event);
            assertEquals(1, store.publishedIds().size());
            Poll created = store.get(store.publishedIds().getFirst());
            assertEquals("Weekend plan", created.title());
            assertEquals("500", created.creatorId());
            assertEquals("300", created.messageId());
            assertEquals(Poll.candidateDates(start, end, EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)), created.dates());
            verify(channel, times(1)).sendMessage(any(MessageCreateData.class));
        }
    }

    @Test void slashPollHasNoArgumentsOrSubcommandsAndDelegatableAdministratorAccess() {
        var command = PollListener.commandData();
        assertTrue(command.getOptions().isEmpty());
        assertTrue(command.getSubcommands().isEmpty());
        assertEquals(Long.valueOf(Permission.ADMINISTRATOR.getRawValue()), command.getDefaultPermissions().getPermissionsRaw());
    }
}
