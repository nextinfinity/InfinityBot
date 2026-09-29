package net.theinfinitymc.infinitybot;

import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.YoutubeSourceOptions;
import dev.lavalink.youtube.clients.*;
import dev.lavalink.youtube.clients.skeleton.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.function.Function;

final class YoutubeConfiguration {
    private static final Logger log = LoggerFactory.getLogger(YoutubeConfiguration.class);

    private YoutubeConfiguration() {}

    static YoutubeAudioSourceManager createSource() {
        Settings settings = parse(System::getenv);
        String cipherUrl = settings.cipherUrl();
        String cipherPassword = settings.cipherPassword();
        String refreshToken = settings.refreshToken();
        String poToken = settings.poToken();
        String visitorData = settings.visitorData();

        YoutubeSourceOptions options = new YoutubeSourceOptions();
        if (cipherUrl != null) {
            options.setRemoteCipher(cipherUrl, cipherPassword, "InfinityBot");
        }

        Web.setPoTokenAndVisitorData(poToken, visitorData);
        WebEmbedded.setPoTokenAndVisitorData(poToken, visitorData);

        List<Client> clients = new ArrayList<>(List.of(new MusicWithThumbnail(),
                new AndroidVrWithThumbnail(), new WebWithThumbnail(), new WebEmbeddedWithThumbnail()));
        if (refreshToken != null) {
            clients.add(new Tv());
        }

        YoutubeAudioSourceManager source = new YoutubeAudioSourceManager(options, clients.toArray(Client[]::new));
        if (refreshToken != null) {
            try {
                // Refresh access tokens automatically; never start an interactive device-login flow.
                source.useOauth2(refreshToken, true);
            } catch (RuntimeException exception) {
                source.shutdown();
                // Do not include the upstream exception: authentication responses can contain secrets.
                throw new IllegalStateException("YouTube OAuth initialization failed. Check YOUTUBE_OAUTH_REFRESH_TOKEN and network access.");
            }
        }

        log.info("YouTube configured: remoteCipher={}, oauth={}, poToken={}, clients=[{}]",
                cipherUrl != null, refreshToken != null, poToken != null,
                clients.stream().map(Client::getIdentifier).collect(Collectors.joining(", ")));
        return source;
    }

    // Parsing is deliberately separate from source construction (which can authenticate).
    record Settings(String cipherUrl, String cipherPassword, String refreshToken,
                    String poToken, String visitorData) {
        @Override
        public String toString() {
            return "YouTube settings [credentials redacted]";
        }
    }

    static Settings parse(Function<String, String> environment) {
        String poToken = environment(environment, "YOUTUBE_PO_TOKEN");
        String visitorData = environment(environment, "YOUTUBE_VISITOR_DATA");
        if ((poToken == null) != (visitorData == null)) {
            throw new IllegalArgumentException("YOUTUBE_PO_TOKEN and YOUTUBE_VISITOR_DATA must both be set or both be empty.");
        }
        return new Settings(environment(environment, "YOUTUBE_REMOTE_CIPHER_URL"),
                environment(environment, "YOUTUBE_REMOTE_CIPHER_PASSWORD"),
                environment(environment, "YOUTUBE_OAUTH_REFRESH_TOKEN"), poToken, visitorData);
    }

    private static String environment(Function<String, String> environment, String name) {
        String value = environment.apply(name);
        return value == null || value.isBlank() ? null : value.strip();
    }
}
