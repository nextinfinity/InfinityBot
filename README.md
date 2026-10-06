# InfinityBot

A Discord music and timer bot using JDA and Lavaplayer.

Commands: `/play` (URL or search), `/pause`, `/skip`, `/stop`, `/queue`, `/volume` (0–100), and `/timer`.

## Timers

The `/timer` command incorporates [TimerBot](https://github.com/nextinfinity/TimerBot)'s timed reminders and optional voice return, useful for activities where users split into separate voice channels and regroup later.

| Parameter | Purpose |
| --- | --- |
| `name` | Required friendly name for the timer. |
| `text-channel` | Required text channel for the start, updates, and completion notice. |
| `length` | Required duration in minutes (minimum 1). |
| `notify-interval` | Optional update interval in minutes (minimum 1); defaults to no intermediate updates. |
| `one-minute-warning` | Optional one-minute warning; off by default. |
| `notify-mention` | Optional user or role to mention in every notice. |
| `return-voice-channel` | Optional voice channel to move **all connected voice users in the server** to at completion. |

For example: `/timer name:Break text-channel:#general length:10 notify-interval:5 one-minute-warning:True`.

An interval update with one minute remaining becomes a single one-minute warning, even if the warning option is off. One-minute timers send only start and completion notices. Voice return runs three seconds after the completion message is successfully sent and uses membership at completion, not when the timer starts.

Timers run in memory: restarting the bot loses pending notices and voice returns. Music commands remain independent of timers.

## Run (with yt-cipher)

Create a Discord application/bot and invite it with the `bot` and `applications.commands` scopes. Grant View Channels, Send Messages, Embed Links, Connect, and Speak in the channels it uses. Timers need View Channels and Send Messages in their target text channel; voice return additionally requires Move Members and access to the destination voice channel. Mentioning roles may require mention permissions. No privileged gateway intents are required.

With Docker Compose installed and the GHCR image published:

```sh
cp .env.example .env
# Edit .env: set your Discord bot token and a long random cipher password.
docker compose pull
docker compose up -d
docker compose logs -f infinitybot
```

This deploys the published InfinityBot image and a private [yt-cipher](https://github.com/kikkia/yt-cipher) service. No cipher port is exposed on the host. The cipher image uses its upstream `master` tag. To update, run `docker compose pull` followed by `docker compose up -d` again.

Images: `ghcr.io/nextinfinity/infinitybot:main` (development), release tags, `sha-<commit>` (short SHA), and `latest` (most recent non-prerelease publication).

## Configuration

| Environment variable | Purpose |
| --- | --- |
| `DISCORD_BOT_TOKEN` | Required Discord bot token. |
| `YOUTUBE_REMOTE_CIPHER_URL` | Cipher base URL; Compose sets `http://yt-cipher:8001`. |
| `YOUTUBE_REMOTE_CIPHER_PASSWORD` | Optional cipher API password; required by the provided Compose stack. |
| `YOUTUBE_OAUTH_REFRESH_TOKEN` | Optional YouTube OAuth **refresh token**; enables the TV playback fallback. |
| `YOUTUBE_PO_TOKEN` | Optional proof-of-origin token for Web clients; requires matching visitor data. |
| `YOUTUBE_VISITOR_DATA` | Companion to `YOUTUBE_PO_TOKEN`; configure both or neither. |

Blank values are treated as unset. Cipher, OAuth, and poToken are independent and can be enabled together. Without remote cipher configured, the bot uses local deciphering.

Clients are tried in this order: Music (search), Android VR, Web, Web Embedded, and TV (only with OAuth configured). OAuth applies to TV playback, while poToken is applied to Web and Web Embedded; configuring both broadens fallback coverage rather than combining credentials on the same client.

OAuth access tokens are refreshed automatically by youtube-source. Initial authorization and replacement of a revoked refresh token remain manual; see [upstream OAuth instructions](https://github.com/lavalink-devs/youtube-source#using-oauth-tokens). The bot does not initiate an interactive login flow. If configured OAuth cannot initialize, startup fails rather than silently disabling it.

poToken/visitor-data pairs are supplied manually and are not automatically generated or renewed. See [upstream poToken instructions](https://github.com/lavalink-devs/youtube-source#using-a-potoken).

Cipher support solves signature deciphering, **not** YouTube IP blocks, age restrictions, or all sign-in challenges. Test playback on the intended deployment host. See [youtube-source remote cipher documentation](https://github.com/lavalink-devs/youtube-source#using-a-remote-cipher-server).

## Healthcheck

The Docker image includes a healthcheck that polls JDA's Discord connection state, checking that the bot is connected rather than just running.

## Tests

Run `./gradlew test` with JDK 25. See [testing guidance](TESTING.md) for scope and conventions.
CI runs tests in a separate job before building or publishing the container.

## Build locally

Requires JDK 25. JDAVE provides Discord voice encryption (DAVE).

```sh
./gradlew clean shadowJar
export DISCORD_BOT_TOKEN='your-token'
export YOUTUBE_REMOTE_CIPHER_URL='http://localhost:8001'
java --enable-native-access=ALL-UNNAMED -jar build/libs/InfinityBot-*-all.jar
```

The standalone command expects a separately reachable cipher server. The Compose cipher is not exposed at localhost. Keep tokens/passwords out of Git; `.env` files are ignored and excluded from Docker builds.
