package net.theinfinitymc.infinitybot.utils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.esotericsoftware.yamlbeans.YamlReader;
import com.esotericsoftware.yamlbeans.YamlWriter;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.User;

public class Config {

	private static String adminid;
	private static String token;
	private static String key;
	private static HashMap<String, String> guildkey = new HashMap<String, String>();
	private static HashMap<String, List<String>> lists = new HashMap<String, List<String>>();
	private static String clientid;
	private static String translatekey;
	private static String googlekey;
	private static String googlecse;

	public static void loadConfig() throws IOException {
		File file = new File("config.yml");
		if (!file.exists()) {
			file.createNewFile();
		}
		FileReader fr = new FileReader("config.yml");
		YamlReader reader = new YamlReader(fr);
		@SuppressWarnings("unchecked")
		Map<String, String> map = (Map<String, String>) reader.read();
		reader.close();
		adminid = map.get("admin-id");
		token = map.get("token");
		key = map.get("key");
		clientid = map.get("soundcloud-client-id");
		translatekey = map.get("translate-api-key");
		googlekey = map.get("google-api-key");
		googlecse = map.get("google-cse");
	}

	public static void loadKeys() throws IOException {
		File file = new File("keys.yml");
		if (!file.exists()) {
			file.createNewFile();
		}
		FileReader fr = new FileReader("keys.yml");
		YamlReader reader = new YamlReader(fr);
		@SuppressWarnings("unchecked")
		Map<String, String> map = (Map<String, String>) reader.read();
		reader.close();
		for (String guild : map.keySet()) {
			guildkey.put(guild, map.get(guild));
		}
	}

	public static void loadLists() throws IOException {
		File file = new File("lists.yml");
		if (!file.exists()) {
			file.createNewFile();
		}
		FileReader fr = new FileReader(file.getAbsolutePath());
		YamlReader reader = new YamlReader(fr);
		@SuppressWarnings("unchecked")
		Map<String, List<String>> map = (Map<String, List<String>>) reader.read();
		reader.close();
		if (map != null && map.keySet() != null) {
			for (String key : map.keySet()) {
				List<String> list = new ArrayList<String>();
				for (String value : map.get(key)) {
					list.add(value);
				}
				lists.put(key, list);
			}
		}
	}

	public static void saveLists() throws IOException {
		FileWriter fw = new FileWriter("lists.yml");
		YamlWriter writer = new YamlWriter(fw);
		writer.write(lists);
		writer.close();
	}

	public static boolean createList(Guild g, String name) {
		String key = g.getId() + "-" + name;
		if (lists.get(key) != null) return false;
		List<String> list = new ArrayList<String>();
		lists.put(key, list);
		return true;
	}

	public static boolean removeList(Guild g, String name) {
		String key = g.getId() + "-" + name;
		if (lists.get(key) == null) return false;
		lists.remove(key);
		try {
			saveLists();
		} catch (Exception e) {
		}
		return true;
	}

	public static String addToList(Guild g, String name, User u) {
		String value = u.getId();
		String key = g.getId() + "-" + name;
		if (lists.get(key) == null) return "That list doesn't exist!";
		List<String> list = lists.get(key);
		if (list.contains(value)) return "That list already contains that player!";
		list.add(value);
		try {
			saveLists();
		} catch (Exception e) {
		}
		return "Added " + u.getName() + " to list " + name + "!";
	}

	public static String removeFromList(Guild g, String name, User u) {
		String value = u.getId();
		String key = g.getId() + "-" + name;
		if (lists.get(key) == null) return "That list doesn't exist!";
		List<String> list = lists.get(key);
		if (!list.contains(value)) return "That list doesn't contain that player!";
		list.remove(value);
		try {
			saveLists();
		} catch (Exception e) {
		}
		return "Removed " + u.getName() + " from list " + name + "!";
	}

	public static List<String> getList(Guild g, String name) {
		return lists.get(g.getId() + "-" + name);
	}

	public static String getId() {
		return clientid;
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
		return googlekey;
	}

	public static String getCSE() {
		return googlecse;
	}

	public static void setKey(String newkey, Guild g) throws IOException {
		FileReader fr = new FileReader("keys.yml");
		YamlReader reader = new YamlReader(fr);
		@SuppressWarnings("unchecked")
		Map<String, String> map = (Map<String, String>) reader.read();
		reader.close();
		map.put(g.getId(), newkey);
		FileWriter fw = new FileWriter("keys.yml");
		YamlWriter writer = new YamlWriter(fw);
		writer.write(map);
		writer.close();
		guildkey.put(g.getId(), newkey);
	}

}
