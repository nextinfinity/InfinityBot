package net.theinfinitymc.infinitybot.utils;

import com.esotericsoftware.yamlbeans.YamlReader;
import com.esotericsoftware.yamlbeans.YamlWriter;
import net.dv8tion.jda.core.entities.Guild;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Config {

	private static String adminid;
	private static String token;
	private static String key;
	private static HashMap<String, String> guildkey = new HashMap<>();
	private static String soundcloudId;
	private static String translatekey;
	private static String googleKey;
	private static String redditUser;
	private static String redditPass;
	private static String redditId;
	private static String redditSecret;

	public static final String DIR = "data";

	public static void loadConfig() throws IOException {
		File file = new File(DIR + File.separator + "config.yml");
		if (!file.exists()) {
			file.createNewFile();
		}
		FileReader fr = new FileReader(DIR + File.separator + "config.yml");
		YamlReader reader = new YamlReader(fr);
		Map<String, String> map = (Map<String, String>) reader.read();
		reader.close();
		if (map != null) {
			token = map.get("token");
			key = map.get("key");
			adminid = map.get("admin-id");
			soundcloudId = map.get("soundcloud-client-id");
			translatekey = map.get("translate-api-key");
			googleKey = map.get("google-api-key");
			redditUser = map.get("reddit-username");
			redditPass = map.get("reddit-password");
			redditId = map.get("reddit-client-id");
			redditSecret = map.get("reddit-secret");
		}
	}

	public static void loadKeys() throws IOException {
		File file = new File(DIR + File.separator + "keys.yml");
		if (!file.exists()) {
			file.createNewFile();
		}
		FileReader fr = new FileReader(DIR + File.separator + "keys.yml");
		YamlReader reader = new YamlReader(fr);
		Map<String, String> map = (Map<String, String>) reader.read();
		reader.close();
		if (map != null) {
			for (String guild : map.keySet()) {
				guildkey.put(guild, map.get(guild));
			}
		}
	}

	public static String getSoundcloudKey() {
		return soundcloudId;
	}

	public static String getAdminId() {
		return adminid;
	}

	public static String getToken() {
		return token;
	}

	public static String getKey(Guild g) {
		if (g == null) {
			return "";
		}
		if (guildkey.containsKey(g.getId())) {
			return guildkey.get(g.getId());
		}
		return key;
	}

	public static String getTranslateKey() {
		return translatekey;
	}

	public static String getGoogleKey() {
		return googleKey;
	}

	public static String getRedditUser() {
		return redditUser;
	}

	public static String getRedditPass() {
		return redditPass;
	}

	public static String getRedditId() {
		return redditId;
	}

	public static String getRedditSecret() {
		return redditSecret;
	}

	public static void setKey(String newkey, Guild g) throws IOException {
		FileReader fr = new FileReader(DIR + File.separator + "keys.yml");
		YamlReader reader = new YamlReader(fr);
		Map<String, String> map = (Map<String, String>) reader.read();
		reader.close();
		map.put(g.getId(), newkey);
		FileWriter fw = new FileWriter(DIR + File.separator + "keys.yml");
		YamlWriter writer = new YamlWriter(fw);
		writer.write(map);
		writer.close();
		guildkey.put(g.getId(), newkey);
	}

}
