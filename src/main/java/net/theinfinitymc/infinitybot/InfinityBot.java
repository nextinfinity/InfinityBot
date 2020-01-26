package net.theinfinitymc.infinitybot;

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.theinfinitymc.infinitybot.utils.Config;
import net.theinfinitymc.infinitybot.utils.JsonHandler;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.login.LoginException;
import java.io.IOException;
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
			api = new JDABuilder(AccountType.BOT).setToken(token).setAudioSendFactory(new NativeAudioSendFactory()).build();
			api.addEventListener(new Listener(api));
			Status.start();
			initLangs();
		} catch (LoginException | IllegalArgumentException | IOException e) {
			e.printStackTrace();
		}

		pool = Executors.newCachedThreadPool();
		audio = new Audio();
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
