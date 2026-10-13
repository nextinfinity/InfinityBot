package net.theinfinitymc.infinitybot.polls;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.restaction.ScheduledEventAction;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/** Owns poll interactions; music/timer commands retain their existing acknowledgement behavior. */
public final class PollListener extends ListenerAdapter implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(PollListener.class);
    private final PollStore store;
    private final PollUpdates updates;
    private volatile JDA jda;

    public PollListener(Path database) { this(new PollStore(database)); }

    PollListener(PollStore store) {
        this.store = store;
        updates = new PollUpdates(store, (poll, done) -> {
            TextChannel channel = jda == null ? null : jda.getTextChannelById(poll.channelId());
            if (channel == null || poll.messageId() == null) { done.run(); return; }
            channel.editMessageById(poll.messageId(), MessageEditData.fromCreateData(PollUi.summary(poll)))
                .queue(message -> done.run(), error -> {
                    LOG.warn("Cannot edit poll {}. Restore permissions and restart if the original message still exists.", poll.id(), error);
                    done.run();
                });
        });
    }

    public static SlashCommandData commandData() {
        return Commands.slash("poll", "Create a date-only availability poll in this text channel")
            .setContexts(InteractionContextType.GUILD)
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR));
    }

    @Override public void onReady(ReadyEvent event) {
        jda = event.getJDA();
        for (String id : store.publishedIds()) updates.request(id);
    }

    @Override public void onShutdown(ShutdownEvent event) { close(); }

    @Override public void close() {
        updates.close();
        store.close();
    }

    private void refresh(String id) { updates.request(id); }

    @Override public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("poll")) return;
        guard(event, () -> {
            creationChannel(event);
            event.replyModal(PollUi.createModal()).queue();
        });
    }

    @Override public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!event.getComponentId().startsWith("poll:")) return;
        guard(event, () -> {
            String[] parts = event.getComponentId().split(":");
            String action = parts[1];
            Poll poll = accessible(event, parts[2]);
            if (Set.of("manage", "cancelconfirm", "cancel", "finalize", "eventform").contains(action)) authorize(event, poll);
            switch (action) {
                case "respond" -> {
                    requireOpen(poll);
                    event.replyModal(PollUi.voteModal(poll, event.getUser().getId())).queue();
                }
                case "eventform" -> {
                    LocalDate date = parseDate(parts[3]);
                    if (!poll.dates().contains(date) || poll.status() == Poll.Status.CANCELLED
                        || (poll.status() == Poll.Status.FINALIZED && !date.equals(poll.chosenDate())))
                        throw new IllegalArgumentException("That final date is no longer valid.");
                    if (poll.eventId() != null || poll.eventState().equals("CREATING"))
                        throw new IllegalArgumentException("An event already exists or creation is in progress.");
                    event.replyModal(PollUi.eventModal(poll, date)).queue();
                }
                case "withdraw" -> {
                    event.deferReply(true).queue();
                    store.withdraw(poll.id(), event.getUser().getId());
                    refresh(poll.id());
                    tell(event, "Your response was withdrawn. You are now counted as not responded.");
                }
                case "details" -> event.reply(PollUi.detailsPicker(poll)).setEphemeral(true).queue();
                case "page" -> event.editMessage(MessageEditData.fromCreateData(
                    PollUi.details(poll, parseDate(parts[3]), Integer.parseInt(parts[4])))).queue();
                case "manage" -> event.reply(PollUi.manage(poll)).setEphemeral(true).queue();
                case "cancelconfirm" -> event.editMessage(MessageEditData.fromCreateData(PollUi.message(
                    "Cancel **" + PollUi.safe(poll.title()) + "**? This closes voting without selecting a date.",
                    ActionRow.of(Button.danger(PollUi.id("cancel", poll.id()), "Confirm cancellation"))
                ))).queue();
                case "cancel" -> {
                    event.deferReply(true).queue();
                    store.cancel(poll.id());
                    refresh(poll.id());
                    tell(event, "Poll cancelled.");
                }
                case "finalize" -> {
                    event.deferReply(true).queue();
                    LocalDate date = parseDate(parts[3]);
                    store.finalizePoll(poll.id(), date);
                    refresh(poll.id());
                    tell(event, "Finalized for **" + PollUi.day(date) + "**. Voting is closed. You can create an event later through Manage.");
                }
                default -> throw new IllegalArgumentException("Unknown poll control.");
            }
        });
    }

    @Override public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (!event.getComponentId().startsWith("poll:")) return;
        guard(event, () -> {
            String[] parts = event.getComponentId().split(":");
            Poll poll = accessible(event, parts[2]);
            LocalDate date = parseDate(event.getValues().getFirst());
            if (parts[1].equals("finaldate")) {
                authorize(event, poll);
                event.editMessage(MessageEditData.fromCreateData(PollUi.confirm(poll, date))).queue();
            } else {
                event.editMessage(MessageEditData.fromCreateData(PollUi.details(poll, date, 0))).queue();
            }
        });
    }

    @Override public void onModalInteraction(ModalInteractionEvent event) {
        if (!event.getModalId().startsWith("poll:")) return;
        event.deferReply(true).queue();
        guard(event, () -> {
            if (event.getModalId().equals("poll:create")) {
                TextChannel channel = creationChannel(event);
                LocalDate start = PollDates.resolve(value(event, "start"), LocalDate.now(Clock.systemUTC()));
                LocalDate end = PollDates.resolve(value(event, "end"), start);
                Poll poll = store.create(event.getGuild().getId(), channel.getId(), event.getUser().getId(), value(event, "title"),
                    start, end, allowedDays(event));
                channel.sendMessage(PollUi.summary(poll)).queue(message -> guard(event, () -> {
                    store.bindMessage(poll.id(), message.getId());
                    refresh(poll.id());
                    tell(event, "Poll created: " + store.get(poll.id()).messageUrl());
                }), error -> {
                    store.deleteUnpublished(poll.id());
                    LOG.warn("Cannot publish poll {}", poll.id(), error);
                    tell(event, "Could not post the poll. Check my View Channel, Send Messages, and Embed Links permissions and try again.");
                });
                return;
            }
            String[] parts = event.getModalId().split(":");
            Poll poll = accessible(event, parts[2]);
            switch (parts[1]) {
                case "vote" -> {
                    Set<LocalDate> available = new HashSet<>();
                    for (int offset = 0; offset < poll.dates().size(); offset += 10)
                        available.addAll(selectedDates(event, "available" + offset / 10));
                    store.vote(poll.id(), event.getUser().getId(), available);
                    refresh(poll.id());
                    tell(event, "Availability saved. Unchecked dates count as unavailable. Use Respond / Edit to change it.");
                }
                case "event" -> {
                    authorize(event, poll);
                    LocalDate date = parseDate(parts[3]);
                    PollEventPlan plan = PollEventPlan.parse(date, value(event, "time"), value(event, "zone"),
                        value(event, "duration"), value(event, "location"), Clock.systemUTC());
                    store.finalizePoll(poll.id(), date);
                    refresh(poll.id());
                    createEvent(event, store.get(poll.id()), plan);
                }
                default -> throw new IllegalArgumentException("Unknown poll form.");
            }
        });
    }

    private void createEvent(IReplyCallback interaction, Poll poll, PollEventPlan plan) {
        Guild guild = interaction.getGuild();
        VoiceChannel voice = plan.location().matches("\\d+") ? guild.getVoiceChannelById(plan.location()) : null;
        if (plan.location().matches("\\d+") && voice == null) {
            tell(interaction, "Date finalized, but that voice channel ID was not found in this server. Use Manage to retry.");
            return;
        }
        if (!guild.getSelfMember().hasPermission(Permission.CREATE_SCHEDULED_EVENTS)) {
            tell(interaction, "Date finalized, but I need Create Events permission. Grant it and use Manage to retry.");
            return;
        }
        if (voice != null && !guild.getSelfMember().hasPermission(voice, Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT)) {
            tell(interaction, "Date finalized, but I need View Channel and Connect in the event's voice channel. Use Manage to retry.");
            return;
        }
        if (!store.claimEvent(poll.id())) {
            tell(interaction, "Date finalized. An event already exists or creation is in progress; check the poll message.");
            return;
        }
        refresh(poll.id());
        String marker = "[InfinityBot poll: " + poll.id() + "]";
        // Reconcile before creation, including after a crash or an ambiguous network failure.
        try {
            guild.retrieveScheduledEvents().queue(events -> {
                try {
                    var existing = events.stream().filter(e -> e.getDescription() != null && e.getDescription().endsWith(marker)
                        && interaction.getJDA().getSelfUser().getId().equals(e.getCreatorId())).findFirst();
                    if (existing.isPresent()) {
                        eventCreated(interaction, poll, existing.get().getId());
                        return;
                    }
                    ScheduledEventAction action = voice == null
                        ? guild.createScheduledEvent(poll.title(), plan.location(), plan.start(), plan.end())
                        : guild.createScheduledEvent(poll.title(), voice, plan.start()).setEndTime(plan.end());
                    action.setDescription("Scheduled from an InfinityBot availability poll.\n" + poll.messageUrl() + "\n" + marker)
                        .queue(created -> guard(interaction, () -> eventCreated(interaction, poll, created.getId())),
                            error -> eventFailed(interaction, poll, error));
                } catch (Exception error) { eventFailed(interaction, poll, error); }
            }, error -> eventFailed(interaction, poll, error));
        } catch (Exception error) { eventFailed(interaction, poll, error); }
    }

    private void eventCreated(IReplyCallback interaction, Poll poll, String eventId) {
        store.eventCreated(poll.id(), eventId);
        refresh(poll.id());
        tell(interaction, "Date finalized. Discord event: https://discord.com/events/" + poll.guildId() + "/" + eventId);
    }

    private void eventFailed(IReplyCallback interaction, Poll poll, Throwable error) {
        LOG.warn("Event creation was not confirmed for poll {}", poll.id(), error);
        store.eventFailed(poll.id());
        refresh(poll.id());
        tell(interaction, "The date is finalized, but event creation was not confirmed. Check my event/channel permissions, then use Manage → Create / retry event. Existing matching events are checked before retrying.");
    }

    private static TextChannel creationChannel(Interaction event) {
        if (!event.isFromGuild() || event.getChannelType() != ChannelType.TEXT)
            throw new IllegalArgumentException("Create polls in a server's standard text channel (not a DM, thread, or forum).");
        TextChannel channel = event.getGuild().getTextChannelById(event.getChannelId());
        if (channel == null || !event.getGuild().getSelfMember().hasPermission(channel,
            Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS))
            throw new IllegalArgumentException("I need View Channel, Send Messages, and Embed Links in this channel.");
        return channel;
    }

    Poll accessible(Interaction event, String id) {
        Poll poll = store.get(id);
        if (!event.isFromGuild() || !poll.guildId().equals(event.getGuild().getId()))
            throw new IllegalArgumentException("That poll is not in this server.");
        TextChannel channel = event.getGuild().getTextChannelById(poll.channelId());
        if (channel == null || event.getMember() == null || !event.getMember().hasPermission(channel, Permission.VIEW_CHANNEL))
            throw new IllegalArgumentException("You must be able to view the poll's original channel.");
        return poll;
    }

    static void authorize(Interaction event, Poll poll) {
        if (event.getMember() == null || !poll.canManage(event.getUser().getId(), event.getMember().hasPermission(Permission.ADMINISTRATOR)))
            throw new IllegalArgumentException("Only the poll creator or a server administrator can manage this poll.");
    }
    private static void requireOpen(Poll poll) {
        if (poll.status() != Poll.Status.OPEN) throw new IllegalArgumentException("This poll is closed.");
    }
    private static String value(ModalInteractionEvent event, String id) {
        return Objects.requireNonNull(event.getValue(id)).getAsString().strip();
    }
    private static Set<DayOfWeek> allowedDays(ModalInteractionEvent event) {
        var mapping = event.getValue("days");
        List<String> values = mapping == null ? List.of() : mapping.getAsStringList();
        return values.stream().map(DayOfWeek::valueOf).collect(Collectors.toSet());
    }
    private static Set<LocalDate> selectedDates(ModalInteractionEvent event, String id) {
        var mapping = event.getValue(id);
        return mapping == null ? Set.of() : mapping.getAsStringList().stream().map(PollListener::parseDate).collect(Collectors.toSet());
    }
    static LocalDate parseDate(String input) {
        try {
            if (!input.matches("\\d{4}-\\d{2}-\\d{2}")) throw new DateTimeParseException("format", input, 0);
            return LocalDate.parse(input);
        } catch (DateTimeParseException e) { throw new IllegalArgumentException("Invalid date. Use YYYY-MM-DD, for example 2026-06-14."); }
    }

    private static void tell(IReplyCallback event, String text) {
        if (event.isAcknowledged())
            event.getHook().editOriginal(text).setEmbeds(List.of()).setComponents(List.of()).setAllowedMentions(List.of()).queue();
        else event.reply(text).setEphemeral(true).setAllowedMentions(List.of()).queue();
    }
    private static void guard(IReplyCallback event, Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException e) { tell(event, e.getMessage()); }
        catch (Exception e) {
            LOG.error("Poll interaction failed", e);
            tell(event, "Something went wrong. The bot operator should check the logs. Saved responses and chosen dates remain stored.");
        }
    }
}
