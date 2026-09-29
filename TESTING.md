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
playback, queue advancement, pause, and stop on the intended host. YouTube/IP,
cipher, and voice transport issues belong to this operational smoke check, not
the deterministic Gradle suite.
