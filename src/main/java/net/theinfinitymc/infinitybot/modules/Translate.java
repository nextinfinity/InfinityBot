package net.theinfinitymc.infinitybot.modules;

import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;

import net.theinfinitymc.infinitybot.InfinityBot;
import net.theinfinitymc.infinitybot.utils.Config;
import net.theinfinitymc.infinitybot.utils.JsonHandler;



public class Translate {
	
	
	public String translate(String s, String l) throws JSONException, IOException{
		String query = format(s);
		String lang = "";
		if(InfinityBot.getLangs().containsKey(l)){
			lang = InfinityBot.getLangs().get(l);
		}else{
			return "An invalid language was specified!";
		}
		String url = ("https://translate.yandex.net/api/v1.5/tr.json/translate?key=" + Config.getTranslateKey() + "&text=" + query + "&lang=" + lang);
		JSONObject results = JsonHandler.readJsonFromUrl(url);
		return results.getJSONArray("text").getString(0);
	}
	
	private String format(String s){
		String formatted = "";
		String[] substrings = s.split(" ");
		for(String sub : substrings){
			if(!sub.equalsIgnoreCase(substrings[0])){
				formatted = formatted + "%20";
			}
			formatted = formatted + sub;
		}
		return formatted;
	}
}
