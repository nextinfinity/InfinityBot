package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.theinfinitymc.infinitybot.InfinityBot;
import net.theinfinitymc.infinitybot.utils.ArgBuilder;
import net.theinfinitymc.infinitybot.utils.Config;
import net.theinfinitymc.infinitybot.utils.JsonHandler;
import org.json.JSONArray;

public class SoundCloud implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		if (args.length > 0) {
			String query = ArgBuilder.buildString(args);
			InfinityBot.getAudio().addToQueue(search(query), event.getGuild(), event.getTextChannel(), event.getAuthor());
		}
	}

	@Override
	public String getDescription() {
		return "Searches SoundCloud and plays the first result.";
	}

	private String search(String query) {
		String key = Config.getSoundcloudKey();
		try {
			query = query.replaceAll(" ", "%20");
			JSONArray results = JsonHandler.readJsonArrayFromUrl("https://api.soundcloud.com/track?client_id=" + key + "&q=" + query);
			return results.getJSONObject(0).getString("permalink_url");
		} catch (Exception e) {
			e.printStackTrace();
			return "Error retrieving search results!";
		}
	}

}
