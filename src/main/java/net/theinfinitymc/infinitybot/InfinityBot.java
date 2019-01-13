package net.theinfinitymc.infinitybot;

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.theinfinitymc.infinitybot.modules.Status;
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
	public static JDA api;
	private static Listener listener;
	private static HashMap<String, String> langs = new HashMap<String, String>();
	private static ExecutorService es;

	public static void main(String[] args) {
		try {
			Config.loadConfig();
			Config.loadKeys();
			Config.loadLists();
			//Phrase.load();
		}catch(IOException e){
			e.printStackTrace();
		}

		String token = Config.getToken();
		
		if(token == null) {
			System.out.println("Please set the token in the config.yml file!");
		}

		try {
			api = new JDABuilder(AccountType.BOT).setToken(token).setAudioSendFactory(new NativeAudioSendFactory()).buildAsync();
			listener = new Listener(api);
			api.addEventListener(listener);
			Status.start();
		} catch (LoginException | IllegalArgumentException e) {
			e.printStackTrace();
		}
		try {
			initLangs("en");
		} catch (IOException e) {
			e.printStackTrace();
		}
		es = Executors.newCachedThreadPool();
	}
	
	private static void initLangs(String l) throws JSONException, IOException{
		String url = ("https://translate.yandex.net/api/v1.5/tr.json/getLangs?key=" + Config.getTranslateKey() + "&ui=" + l);
		JSONObject results = JsonHandler.readJsonFromUrl(url);
		JSONObject lang = results.getJSONObject("langs");
		for(String entry : lang.keySet()){
		    langs.put(lang.getString(entry).toLowerCase(), entry.toLowerCase());
		}
	}

	public static ExecutorService getThreadPool(){
		return es;
	}

	public static Listener getListener(){
		return listener;
	}
	
	public static HashMap<String, String> getLangs(){
		return langs;
	}
}
