# InfinityBot

A Discord music bot using JDA and Lavaplayer.

Commands: `/play` (URL or search), `/pause`, `/skip`, `/stop`, `/queue`, and `/volume` (0–100).

## Run (with yt-cipher)

Create a Discord application/bot and invite it with the `bot` and `applications.commands` scopes. Grant View Channels, Send Messages, Embed Links, Connect, and Speak in the channels it uses. No privileged gateway intents are required.

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

OAuth access tokens are refreshed automatically by youtube-source. Initial authorization and replacement of a revoked refresh token remain manual; see [upstream OAuth instructions](https://github.com/lavalink-devs/youtube-source#using-oauth-tokens). Use a burner account, not your primary account: upstream warns of account-termination risk. The bot does not initiate an interactive login flow. If configured OAuth cannot initialize, startup fails rather than silently disabling it.

poToken/visitor-data pairs are supplied manually and are not automatically generated or renewed. See [upstream poToken instructions](https://github.com/lavalink-devs/youtube-source#using-a-potoken). An incomplete pair fails startup before connecting to Discord. After changing `.env`, run `docker compose up -d` to recreate the bot with the new values.

Startup logs show enabled authentication modes and client order without credential values. Playback failures/stalls produce a short guild-scoped warning; a failed Discord notification does not prevent queue advancement. There are no health probes or aggregate failure alerts. Avoid enabling upstream DEBUG logging or sharing unredacted upstream errors: these may contain credentials or signed playback URLs.

Cipher support solves signature deciphering, **not** YouTube IP blocks, age restrictions, or all sign-in challenges. Test playback on the intended deployment host. See [youtube-source remote cipher documentation](https://github.com/lavalink-devs/youtube-source#using-a-remote-cipher-server).

## Build locally

Requires JDK 25. JDAVE provides Discord voice encryption (DAVE).

```sh
./gradlew clean shadowJar
export DISCORD_BOT_TOKEN='your-token'
export YOUTUBE_REMOTE_CIPHER_URL='http://localhost:8001'
java --enable-native-access=ALL-UNNAMED -jar build/libs/InfinityBot-*-all.jar
```

The standalone command expects a separately reachable cipher server. The Compose cipher is not exposed at localhost. Keep tokens/passwords out of Git; `.env` files are ignored and excluded from Docker builds.
