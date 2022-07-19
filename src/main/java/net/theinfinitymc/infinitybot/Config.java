package net.theinfinitymc.infinitybot;

import com.esotericsoftware.yamlbeans.YamlReader;
import lombok.Value;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

@Value
public class Config {
	String token;
	String googleKey;
	static String DIR = "data";

	public Config() throws IOException, InstantiationException {
		String path = getPath("config.yml");
		File file = new File(path);
		if (!file.exists()) {
			file.createNewFile();
		}
		FileReader fr = new FileReader(path);
		YamlReader reader = new YamlReader(fr);
		Map<String, String> map = (Map<String, String>) reader.read();
		reader.close();
		if (map != null && map.containsKey("token")) {
			this.token = map.get("token");
			this.googleKey = map.get("google-api-key");
		} else {
			throw new InstantiationException("Failed to load config values. Please set token in config.yml.");
		}
	}

	private static String getPath(String file) {
		try {
			String currentDir = new File(InfinityBot.class.getProtectionDomain().getCodeSource().getLocation()
					.toURI()).getPath();
			currentDir = currentDir.substring(0, currentDir.lastIndexOf('\\'));
			return currentDir + File.separator + DIR + File.separator + file;
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}
}
