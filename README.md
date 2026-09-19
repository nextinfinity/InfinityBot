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

Without remote cipher configured, the bot uses local deciphering.

Cipher support solves signature deciphering, **not** YouTube IP blocks, age restrictions, or all sign-in challenges. Test playback on the intended deployment host. See [youtube-source remote cipher documentation](https://github.com/lavalink-devs/youtube-source#using-a-remote-cipher-server).

## Release versioning

Publish a GitHub release with a tag such as `v3.2.0`. The workflow strips the leading `v` and passes the version through Docker's `APP_VERSION` build argument to Gradle's `appVersion` property. The JAR filename and `Implementation-Version` manifest entry then use `3.2.0`; no manual Gradle version bump is needed. Tags without `v` and prerelease suffixes (e.g. `3.2.0-rc.1`) also work.

Local, branch/PR, and manual workflow builds default to `0.0.0-SNAPSHOT`, including manual runs on tags. Override locally with `./gradlew clean shadowJar -PappVersion=3.2.0` or `docker build --build-arg APP_VERSION=3.2.0 -t infinitybot:3.2.0 .`. Image tagging is unchanged; only published non-prereleases update `latest`.

## Build locally

Requires JDK 25. JDAVE provides Discord voice encryption (DAVE).

```sh
./gradlew clean shadowJar
export DISCORD_BOT_TOKEN='your-token'
export YOUTUBE_REMOTE_CIPHER_URL='http://localhost:8001'
java --enable-native-access=ALL-UNNAMED -jar build/libs/InfinityBot-*-all.jar
```

The standalone command expects a separately reachable cipher server. The Compose cipher is not exposed at localhost. Keep tokens/passwords out of Git; `.env` files are ignored and excluded from Docker builds.
