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

final class YoutubeConfiguration {
    private static final Logger log = LoggerFactory.getLogger(YoutubeConfiguration.class);

    private YoutubeConfiguration() {}

    static YoutubeAudioSourceManager createSource() {
        String cipherUrl = environment("YOUTUBE_REMOTE_CIPHER_URL");
        String cipherPassword = environment("YOUTUBE_REMOTE_CIPHER_PASSWORD");
        String refreshToken = environment("YOUTUBE_OAUTH_REFRESH_TOKEN");
        String poToken = environment("YOUTUBE_PO_TOKEN");
        String visitorData = environment("YOUTUBE_VISITOR_DATA");

        YoutubeSourceOptions options = new YoutubeSourceOptions();
        if (cipherUrl != null) {
            options.setRemoteCipher(cipherUrl, cipherPassword, "InfinityBot");
        }

        if ((poToken == null) != (visitorData == null)) {
            throw new IllegalArgumentException("YOUTUBE_PO_TOKEN and YOUTUBE_VISITOR_DATA must both be set or both be empty.");
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

    private static String environment(String name) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? null : value.strip();
    }
}
