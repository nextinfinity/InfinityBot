package net.theinfinitymc.infinitybot;

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.theinfinitymc.infinitybot.utils.Config;
import net.theinfinitymc.infinitybot.utils.JsonHandler;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InfinityBot {

	private static JDA api;
	private static HashMap<String, String> langs = new HashMap<>();
	private static ExecutorService pool;
	private static Audio audio;

	public static void main(String[] args) {
		try {
			Config.loadConfig();
			Config.loadKeys();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String token = Config.getToken();

		if (token == null) {
			System.out.println("Please set the token in the config.yml file!");
		}

		try {

			api = JDABuilder.createDefault(token, InfinityBot.getIntents())
					.addEventListeners(new Listener(api))
					.setAudioSendFactory(new NativeAudioSendFactory())
					.build();
			Status.start();
			initLangs();
		} catch (LoginException | IllegalArgumentException | IOException e) {
			e.printStackTrace();
		}

		pool = Executors.newCachedThreadPool();
		audio = new Audio();
	}

	private static Collection<GatewayIntent> getIntents(){
		Collection<GatewayIntent> gi = new ArrayList<>();
		gi.add(GatewayIntent.DIRECT_MESSAGE_REACTIONS);
		gi.add(GatewayIntent.DIRECT_MESSAGE_TYPING);
		gi.add(GatewayIntent.DIRECT_MESSAGES);
		gi.add(GatewayIntent.GUILD_BANS);
		gi.add(GatewayIntent.GUILD_EMOJIS);
		gi.add(GatewayIntent.GUILD_INVITES);
		gi.add(GatewayIntent.GUILD_MESSAGE_REACTIONS);
		gi.add(GatewayIntent.GUILD_MESSAGE_TYPING);
		gi.add(GatewayIntent.GUILD_MESSAGES);
		gi.add(GatewayIntent.GUILD_VOICE_STATES);
		return gi;
	}

	private static void initLangs() throws JSONException, IOException {
		String url = ("https://translate.yandex.net/api/v1.5/tr.json/getLangs?key=" + Config.getTranslateKey() + "&ui=en");
		JSONObject results = JsonHandler.readJsonFromUrl(url);
		JSONObject lang = results.getJSONObject("langs");
		for (String entry : lang.keySet()) {
			langs.put(lang.getString(entry).toLowerCase(), entry.toLowerCase());
		}
	}

	static JDA getAPI() {
		return api;
	}

	public static ExecutorService getThreadPool() {
		return pool;
	}

	public static HashMap<String, String> getLangs() {
		return langs;
	}

	public static Audio getAudio() {
		return audio;
	}
}
