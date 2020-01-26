package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.theinfinitymc.infinitybot.InfinityBot;
import net.theinfinitymc.infinitybot.utils.ArgBuilder;
import net.theinfinitymc.infinitybot.utils.Config;
import net.theinfinitymc.infinitybot.utils.JsonHandler;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;

public class Translate implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		if (args.length >= 2) {
			String query = ArgBuilder.buildString(Arrays.copyOfRange(args, 1, args.length - 1));
			final String lang = args[0].toLowerCase();
			InfinityBot.getThreadPool().execute(() -> {
				try {
					event.getChannel().sendMessage(translate(query, lang)).queue();
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}
	}

	@Override
	public String getDescription() {
		return "Translates text to specified language.";
	}

	private String translate(String s, String l) throws JSONException, IOException {
		String query = format(s);
		String lang;
		if (InfinityBot.getLangs().containsKey(l)) {
			lang = InfinityBot.getLangs().get(l);
		} else {
			return "An invalid language was specified!";
		}
		String url = ("https://translate.yandex.net/api/v1.5/tr.json/translate?key=" + Config.getTranslateKey() + "&text=" + query + "&lang=" + lang);
		JSONObject results = JsonHandler.readJsonFromUrl(url);
		return results.getJSONArray("text").getString(0);
	}

	private String format(String s) {
		StringBuilder formatted = new StringBuilder();
		String[] substrings = s.split(" ");
		for (String sub : substrings) {
			if (!sub.equalsIgnoreCase(substrings[0])) {
				formatted.append("%20");
			}
			formatted.append(sub);
		}
		return formatted.toString();
	}

}
