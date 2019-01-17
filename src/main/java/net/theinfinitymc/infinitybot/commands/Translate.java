package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.theinfinitymc.infinitybot.InfinityBot;
import net.theinfinitymc.infinitybot.utils.Config;
import net.theinfinitymc.infinitybot.utils.JsonHandler;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class Translate implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		if(args.length >= 3){
			int i = 3;
			String m = args[2];
			while(i < args.length){
				m = m + " " + args[i];
				i++;
			}
			final String query = m;
			final String lang = args[1].toLowerCase();
			InfinityBot.getThreadPool().execute(new Runnable(){
				@Override
				public void run(){
					try {
						e.getChannel().sendMessage(translate(query, lang)).queue();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
	}

	@Override
	public String getDescription() {
		return null;
	}

	public String translate(String s, String l) throws JSONException, IOException {
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
