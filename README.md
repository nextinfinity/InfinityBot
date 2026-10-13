# InfinityBot

A Discord music, timer, and scheduling bot using JDA and Lavaplayer.

Commands: `/play` (URL or search), `/pause`, `/skip`, `/stop`, `/queue`, `/volume` (0–100), `/timer`, and `/poll`.

## Scheduling

`/poll` opens a form for an event title, inclusive date range, and seven
weekday checkboxes (all checked initially; maximum 25 candidate dates). Dates
accept `M/D` or `MM-DD` with the year inferred, or explicit `YYYY-MM-DD`.
Members check the dates they can attend; unchecked dates count as unavailable.
Responses are editable, and the original message shows a live availability list,
highlighting the strongest dates. Details shows who can attend each date.

The creator or a server administrator can finalize a date or cancel the poll.
Optional Discord event creation collects time, timezone, duration, and voice
channel/location at finalization. All results and controls live on the original
poll message; deleting it makes the poll inaccessible. `/poll` defaults to
Administrator access, delegatable through Discord command permissions; members
vote through the message buttons.

Polls persist in SQLite across restarts. Compose mounts a persistent `poll-data`
volume; do not delete it when updating. Poll messages need View Channel, Send
Messages, and Embed Links. Optional events need Create Events, plus View Channel
and Connect for voice events. No additional privileged intents are needed.

See [scheduling details and live-test instructions](docs/scheduling.md), including
`./scripts/run-local.sh` for a temporary bot test without Docker.

## Run (with yt-cipher)

Create a Discord application/bot and invite it with the `bot` and `applications.commands` scopes. Grant View Channels, Send Messages, Embed Links, Connect, and Speak in the channels it uses. Timers need View Channels and Send Messages in their target text channel; voice return additionally requires Move Members in every source voice/stage channel and View Channel/Connect in the destination voice channel. Mentioning roles may require mention permissions. No privileged gateway intents are required.

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
| `POLL_DATABASE_PATH` | SQLite poll database; standalone default `data/polls.sqlite`, Compose uses `/app/data/polls.sqlite` in a named volume. |
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
