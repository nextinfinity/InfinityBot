# Scheduling polls

## Member and admin flow

1. An admin runs `/poll` in a standard server text channel. A private form
   asks for a title, start date, inclusive end date, and seven weekday checkboxes
   (Monday–Sunday), all checked by default. Uncheck weekdays to exclude; keep at
   least one checked. For weekends only, leave Saturday and Sunday checked.
   The resulting poll is public in that channel. There can be at most **25
   candidate dates**, within a range of at most 90 days.
   Dates accept `M/D` or `MM-DD` without a year: start resolves to its next
   occurrence on or after today **in UTC**, and end resolves on or after start.
   For example, `12/28`–`1/3` crosses into the following year. `YYYY-MM-DD` still
   works and preserves the explicit year. February 29 must be valid in the inferred
   year; otherwise specify an explicit leap year. Resolved years appear in the
   published poll. Availability is date-only: members should agree separately
   on the usual time.
2. Members click **Respond / Edit**. A private form has checkboxes for the dates
   they can attend. On submission, unchecked dates count as unavailable; leaving
   everything unchecked explicitly records that no date works. Existing answers
   are checked when reopening the form. Discord limits each checkbox group to 10
   dates, so longer polls use up to three groups in one form, submitted together.
3. The original message updates with a chronological availability list such as
   **Sat, Jun 6, 2026 — 4 of 6 available**. Stars and bold dates highlight the most
   available responses, preserving ties. **Details** displays who is available or
   unavailable on a selected date, in pages of 15 members. **Withdraw** removes
   your entire response. Members who have not submitted a response (or withdrew
   it) do not count as unavailable and are not listed in Details.
4. The creator or a server administrator clicks **Manage**, chooses a candidate
   date, and confirms **Finalize date only** or **Finalize + create event**. This
   locks voting permanently. **Cancel poll** closes without choosing a date;
   cancellation also requires confirmation. Closed polls retain results/details.

All results and controls are accessed from the original poll message. There is
no results lookup command or visible poll UUID. If the message is deleted, the
poll is inaccessible in Discord; create a new poll if needed. Original-message
updates are batched briefly and serialized per poll, so simultaneous responses
cannot make an older edit overwrite newer results.

`/poll` defaults to **Administrator** access. Server admins can delegate the
command through Discord's command permissions, just as with `/timer`; delegated
members can create polls and manage their own polls. Voting/details buttons are
available to all members who can view the poll's original channel. Management
buttons and submitted forms independently enforce creator/admin access. All
lookups and interactions enforce the original guild and channel visibility.
If the original channel is deleted, the poll can no longer be accessed in Discord.

Creation in DMs, threads, forums, and announcement channels is not supported in
v1. There are no reminders, automatic closing deadlines, reopening, individual
candidate-date exclusions, or automatic winner selection.

## Optional Discord scheduled events

Event creation is optional at finalization, or later through **Manage → Create /
retry event**. The form asks for:

- A start time in 24-hour `HH:mm` format.
- An IANA timezone such as `America/New_York`, `Europe/London`, or `UTC`.
- A duration in minutes (1–10080; default 180), supplying the event end time.
- A voice channel ID **from this server**, or an external URL/location. Enable
  Discord Developer Mode and right-click the voice channel to copy its ID.
  A digits-only entry is interpreted as a voice channel ID; stage events are not
  supported. Any other entry becomes the external location.

The chosen date is interpreted in that timezone. The start must be in the future.
Skipped and ambiguous daylight-saving times are rejected rather than guessed;
choose another local time or express the time in UTC. Invalid form values do not
finalize the poll. Once valid values are submitted, finalization is saved before
attempting event creation. Permission/API failures leave that chosen date intact.
Time, timezone, duration, and location inputs are not saved for future retries;
reenter them when retrying.

The bot needs **Create Events** permission. For voice events it also needs **View
Channel** and **Connect** in the target voice channel. It doesn't join voice or
subscribe voters to the event. Discord's normal event RSVP remains separate from
poll availability.

Event creation is claimed in storage to prevent simultaneous duplicate requests.
Retries first retrieve server scheduled events and look for this bot's event with
an exact poll marker at the end of its description. Interrupted claims become
retryable on restart. Leave the marker intact so reconciliation works after an
ambiguous network failure. This is best-effort reconciliation, not a distributed
transaction: manually editing/removing the marker or cancelling an unrecorded
event can defeat it. Successfully recorded events aren't recreated automatically
if someone later deletes them.

## Persistence and deployment

SQLite stores poll metadata, candidate dates, user IDs, responses, chosen dates,
and event state. The default standalone path is `data/polls.sqlite`, configurable
with `POLL_DATABASE_PATH`. The parent directory is created on startup. Database
open failures stop startup rather than silently discarding persistence.

Compose uses `/app/data/polls.sqlite` in the `poll-data` named volume. The image
prepares this directory for its non-root UID/GID 10001. With a bind mount instead,
ensure that UID can write the directory. **Do not use `docker compose down -v`
unless you intend to delete saved polls.** Keep backups; for a simple consistent
backup, stop the bot and copy the data directory (including SQLite's sidecars).
Run only one bot process against a database. Do not copy production member
availability into a temporary development bot unnecessarily.

Poll updates edit messages through the normal channel API, not expiring
interaction hooks. Persisted message IDs/components continue to work after a
restart. Startup refreshes known poll messages; deleted messages or lost bot
permissions are logged. Restore permissions and restart to refresh messages
that still exist; deleted messages are not recreated. Polls are not automatically
purged: operators should treat the database and backups as member-availability data.

The published GHCR images don't contain this feature until this branch is merged
and a new image is built/published. To test this branch, use the standalone script
below or build your own image rather than pulling the current release.

## Small live test (no Docker required)

Use a **separate temporary Discord application**, not the production bot. Startup
replaces the application's slash command definitions; a separate app also avoids
two processes competing for production interactions.

1. In the Discord Developer Portal, create an application and bot; obtain its
   token. Leave privileged gateway intents disabled. Use a **server/Guild
   installation**, with `bot` and `applications.commands` scopes.
2. Invite it to a small test server. Grant **View Channel**, **Send Messages**,
   **Embed Links**, and **Create Events**. Add **View Channel/Connect** in a test
   voice channel if testing voice events. Bot Administrator is not necessary.
3. Run from this checkout:

   ```sh
   ./scripts/run-local.sh
   ```

   This runs tests and builds `build/libs/InfinityBot-local-smoke-all.jar`, then
   asks for the token using a **hidden terminal prompt**. The token isn't written
   to a file or placed in shell history; do not paste it into chat. An already
   exported `DISCORD_BOT_TOKEN` is also supported. A JDK 25 installation is needed;
   set `JAVA_HOME` if necessary. On the prepared local machine, the script finds
   `$HOME/.cache/infinitybot-jdk25/Contents/Home` automatically.

   Polling needs no YouTube credentials or cipher service. Music playback still
   has its usual requirements. The test database defaults to
   `data/live-test.sqlite`, isolated from `data/polls.sqlite`. Ctrl-C stops the
   process. Rerun the script with the same token/database for the restart check.
   Global slash commands may take a little time to appear; refresh Discord if
   necessary. If port 8080 is already occupied, stop the other local bot first.

4. Test with a future date range in a normal text channel:
   - Create an all-days poll, a weekends-only poll, and a custom weekday poll.
     Check checkbox defaults, inclusive boundaries, shorthand dates/year rollover,
     and explicit-year dates.
   - Check available dates; verify the live availability list and strongest dates.
   - Reopen/edit, uncheck every date, and withdraw. Try a poll with more than 10
     dates to verify all checkbox groups save together.
   - Use a second member for totals, visible Details, and denied management.
   - Make two polls and verify responses stay attached to the correct message.
   - Restart the bot; verify previous votes and old buttons still work.
   - Finalize one poll without an event; verify voting is disabled, including a
     voting form opened before closing. Cancel another poll.
   - Finalize with an external event, then a voice event. Verify date/time/timezone
     in Discord, end time, title/location, event link, and no duplicate creation.
   - Remove Create Events, finalize another poll with event creation, verify the
     chosen date remains, restore permission, and retry via Manage.
   - Test a phone as well as desktop, especially unchecked/grouped checkbox values.
   - If desired, delete a poll message and verify there is no recovery/lookup
     command; a new poll must be created.

5. Stop the temporary bot and revoke/reset its token when finished. Remove the
   test bot from the server and delete its test database if no longer needed.
