# InfinityBot

A Discord music bot using JDA and Lavaplayer.

Commands: `/play` (URL or search), `/pause`, `/skip`, `/stop`, `/queue`, and `/volume` (0–100).

## Run with yt-cipher

Create a Discord application/bot and invite it with the `bot` and `applications.commands` scopes. Grant View Channels, Send Messages, Embed Links, Connect, and Speak in the channels it uses. No privileged gateway intents are required.

With Docker Compose installed and the GHCR image published:

```sh
cp .env.example .env
# Edit .env: set your Discord bot token and a long random cipher password.
docker compose pull
docker compose up -d
docker compose logs -f infinitybot
```

This deploys the published InfinityBot image and a private [yt-cipher](https://github.com/kikkia/yt-cipher) service; Compose does not build images locally. No cipher port is exposed on the host, and no YouTube OAuth token is needed. The cipher image uses its upstream `main` tag; pin a tested digest for production. To update, run `docker compose pull` followed by `docker compose up -d` again.

Images: `ghcr.io/nextinfinity/infinitybot:main` (development), release tags, `sha-<commit>` (short SHA), and `latest` (most recent non-prerelease publication). CI builds Linux amd64 images. Initial GHCR packages may need to be made public in package settings for anonymous pulls.

## Configuration

| Environment variable | Purpose |
| --- | --- |
| `DISCORD_BOT_TOKEN` | Required Discord bot token. |
| `YOUTUBE_REMOTE_CIPHER_URL` | Cipher base URL; Compose sets `http://yt-cipher:8001`. |
| `YOUTUBE_REMOTE_CIPHER_PASSWORD` | Optional cipher API password; required by the provided Compose stack. |
| `YOUTUBE_OAUTH_TOKEN` | Optional legacy refresh token, used only when no remote cipher URL is set. |

Remote cipher takes precedence over OAuth. With neither configured, the bot uses local deciphering without an interactive OAuth flow.

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

## CI

Pull requests build without publishing. Pushes to `main`, published releases, and manual runs build and publish to GHCR including provenance attestations. Manual runs publish tags for the selected ref, not `latest`.
