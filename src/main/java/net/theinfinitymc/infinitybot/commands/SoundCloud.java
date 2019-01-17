package net.theinfinitymc.infinitybot.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.theinfinitymc.infinitybot.utils.JsonHandler;
import org.json.JSONArray;

public class SoundCloud implements Command {

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		if(args.length >= 2){
			int i = 3;
			String m = args[2];
			while(i < args.length){
				m = m + " " + args[i];
				i++;
			}
			final String query = m;
			attemptAddToQueue(search(query), e.getGuild(), e.getTextChannel(), e.getAuthor());
		}
	}

	@Override
	public String getDescription() {
		return null;
	}

	private String search(String query) {
		try{
			query = query.replaceAll(" ", "%20");
			JSONArray results = JsonHandler.readJsonArrayFromUrl("https://api.soundcloud.com/track?client_id=35e00568851d1294e9816df13a80b987&q=" + query);
			return results.getJSONObject(0).getString("permalink_url");
		}catch(Exception e){
			e.printStackTrace();
			return "Error retrieving search results!";
		}
	}

}
