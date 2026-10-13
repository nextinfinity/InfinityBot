#!/usr/bin/env bash
# Standalone polling smoke test; no Docker, cipher service, or stored token required.
set -euo pipefail
cd "$(dirname "$0")/.."

if [[ -z "${JAVA_HOME:-}" && -x "$HOME/.cache/infinitybot-jdk25/Contents/Home/bin/java" ]]; then
    export JAVA_HOME="$HOME/.cache/infinitybot-jdk25/Contents/Home"
fi
if [[ -n "${JAVA_HOME:-}" ]]; then
    export PATH="$JAVA_HOME/bin:$PATH"
fi
if ! command -v java >/dev/null || ! java -version >/dev/null 2>&1; then
    echo 'JDK 25 is required. Install it and set JAVA_HOME.' >&2
    exit 1
fi

# Build before accepting the secret, so Gradle never needs to inherit the token.
TOKEN="${DISCORD_BOT_TOKEN:-}"
export -n TOKEN
unset DISCORD_BOT_TOKEN
sh ./gradlew --no-daemon -PappVersion=local-smoke check shadowJar
if [[ -z "$TOKEN" ]]; then
    if [[ ! -t 0 ]]; then
        echo 'Run interactively to enter a token, or set DISCORD_BOT_TOKEN.' >&2
        exit 1
    fi
    IFS= read -r -s -p 'Temporary Discord bot token (hidden): ' TOKEN
    printf '\n'
fi
if [[ -z "$TOKEN" ]]; then
    echo 'No token entered.' >&2
    exit 1
fi
export DISCORD_BOT_TOKEN="$TOKEN"
unset TOKEN
# Isolate live-test data from a normal standalone deployment.
export POLL_DATABASE_PATH="${POLL_DATABASE_PATH:-data/live-test.sqlite}"
echo 'Starting bot. Ctrl-C to stop. Do not share the token or delete the database between restart tests.'
exec java --enable-native-access=ALL-UNNAMED -jar build/libs/InfinityBot-local-smoke-all.jar
