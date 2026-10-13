# Testing

Run `./gradlew test` (or `sh ./gradlew test`) with JDK 25. `./gradlew check`
includes the same suite. Reports are in `build/reports/tests/test/index.html`.
CI runs `check` in a separate Test job; container builds/publication depend on it.

## Scope

JUnit Jupiter and Mockito test our decisions, not the external services:

- `GuildAudioTest`: immediate playback versus FIFO queueing, skip/stop/pause,
  exhausted or failed next-track handling, track-end policies, and advancement
  despite failed error notifications.
- `AudioManagerTest`: one search fallback, direct playlists versus search
  results, request metadata, queue outcomes, failure cleanup without interrupting
  existing playback, and voice connection policy.
- `YoutubeConfigurationTest`: pure environment parsing, blank/trimmed values,
  independent optional settings, paired poToken/visitor data, and credential
  redaction in parsed settings' string representation and validation errors.
- `TimerTest`: default and interval notice schedules, one-minute warning
  deduplication, mentions, guild-only invocation, and completion-time voice return
  for connected members only, Administrator defaults without caller enforcement,
  and bot-permission preflight (text, destination voice, source voice/stage, and
  non-mentionable roles). Scheduled notices and completion callbacks are
  captured without waiting or contacting Discord.

- Scheduling tests (`polls/`): shorthand date/year inference and rollover,
  weekday checkbox defaults, inclusive/filter boundaries, 25-date limits,
  availability counts and ranking, vote replacement/withdrawal, closed-poll
  rejection, permissions/guild/channel isolation, and modal acknowledgement.
  Temporary real SQLite databases cover persistence, concurrent voters,
  finalization/cancellation, event claims and crash recovery. Mocked Discord
  callbacks cover event creation, failure, reconciliation, and duplicate-request
  prevention. A manually driven scheduler checks batched/in-flight message
  updates; UI boundary tests check checkbox grouping at 10/20-date boundaries,
  maximum-size forms, preselected answers, and paginated details.

Tests inject the loader and activity callback, manually deliver load/playback
callbacks, and parse configuration from a map. They never construct a connected
bot or authenticate with YouTube. Mockito is supplied as an explicit test JVM
agent for modern JDKs; no application runtime dependency is added.

## Keeping this lightweight

- Add tests for plausible bugs in application behavior, not a coverage target.
- Prefer parameterized boundary cases and observable outcomes. Interaction
  assertions are appropriate for decisions such as disconnecting or advancing
  exactly once, not incidental builder call order.
- No sleeps, network, credentials, real audio, or whole-bot startup in tests.
- Do not test Discord delivery/voice transport, YouTube availability, cipher or
  OAuth refresh, decoding, library internals, reflection, generated accessors,
  trivial command delegation, or exact prose/embed styling.
- Keep fixtures local and small; do not build a Discord/Lavaplayer simulator.
- Add a focused regression test when fixing a substantive bug.

After deployment or dependency updates, manually check Discord connectivity,
playback, queue advancement, pause, and stop on the intended host. Also create a
short `/timer` with updates, a mention, and a voice-return destination; verify
notice delivery and voice movement with the bot's actual channel permissions.
YouTube/IP, cipher, and voice transport issues belong to this operational smoke check, not
the deterministic Gradle suite.

For scheduling, follow the [temporary-bot live-test checklist](docs/scheduling.md#small-live-test-no-docker-required).
`./scripts/run-local.sh` runs the suite, builds a shaded jar, and prompts privately
for a temporary bot token. Use a distinct application so command registration and
live interactions do not interfere with the production bot. Verify forms on both
desktop and mobile: Discord client rendering and real API permission enforcement
are intentionally outside the unit tests.
