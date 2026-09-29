package net.theinfinitymc.infinitybot;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class YoutubeConfigurationTest {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void absentAndBlankValuesAreUnset(String value) {
        assertEquals(new YoutubeConfiguration.Settings(null, null, null, null, null),
                YoutubeConfiguration.parse(ignored -> value));
    }

    @ParameterizedTest
    @CsvSource({"true,false,false", "false,true,false", "false,false,true", "true,true,true"})
    void optionalFeaturesAreIndependentAndValuesAreTrimmed(boolean cipher, boolean oauth, boolean poToken) {
        Map<String, String> env = new HashMap<>();
        if (cipher) {
            env.put("YOUTUBE_REMOTE_CIPHER_URL", " https://cipher.example ");
            env.put("YOUTUBE_REMOTE_CIPHER_PASSWORD", " password-secret ");
        }
        if (oauth) env.put("YOUTUBE_OAUTH_REFRESH_TOKEN", " oauth-secret ");
        if (poToken) {
            env.put("YOUTUBE_PO_TOKEN", " po-secret ");
            env.put("YOUTUBE_VISITOR_DATA", " visitor-secret ");
        }
        var settings = YoutubeConfiguration.parse(env::get);
        assertEquals(new YoutubeConfiguration.Settings(cipher ? "https://cipher.example" : null,
                cipher ? "password-secret" : null, oauth ? "oauth-secret" : null,
                poToken ? "po-secret" : null, poToken ? "visitor-secret" : null), settings);
        assertFalse(settings.toString().contains("secret"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"YOUTUBE_PO_TOKEN", "YOUTUBE_VISITOR_DATA"})
    void rejectsHalfConfiguredTokenPairWithoutLeakingItsValue(String key) {
        var env = Map.of(key, "sensitive-value");
        var exception = assertThrows(IllegalArgumentException.class, () -> YoutubeConfiguration.parse(env::get));
        assertFalse(exception.getMessage().contains("sensitive-value"));
    }
}
