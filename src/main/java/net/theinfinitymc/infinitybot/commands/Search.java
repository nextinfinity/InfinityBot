package net.theinfinitymc.infinitybot.commands;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.theinfinitymc.infinitybot.AudioManager;
import net.theinfinitymc.infinitybot.Command;
import net.theinfinitymc.infinitybot.QueueCallback;

import java.util.List;
import java.util.Objects;

public class Search extends Command {

	private final com.google.api.services.youtube.YouTube youtube;
	private final String key;
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	public Search(AudioManager audioManager) {
		super(
				audioManager,
				"search",
				"Searches YouTube for the query and plays the first result.",
				List.of(
						new OptionData(OptionType.STRING, "query", "The search term for YouTube")
								.setRequired(true)
				)
		);
		this.youtube = new com.google.api.services.youtube.YouTube.Builder(new NetHttpTransport(), JSON_FACTORY,
				request -> {}).setApplicationName("infinitybot-youtube-search").build();
		this.key = System.getenv("YOUTUBE_API_KEY");
	}

	public void execute(SlashCommandInteractionEvent event) {
		String query = Objects.requireNonNull(event.getOption("query")).getAsString();
		String result = search(query);
		getAudioManager().tryAddToQueue(result, event.getGuild(), event.getChannel(), event.getUser(),
				new QueueCallback(event.getHook(), result));
	}

	private String search(String query) {
		try {
			com.google.api.services.youtube.YouTube.Search.List search = youtube.search().list("id,snippet");
			search.setKey(key);
			search.setQ(query);
			search.setType("video");
			search.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)");
			search.setMaxResults(1L);

			SearchListResponse searchResponse = search.execute();
			List<SearchResult> searchResultList = searchResponse.getItems();

			return "https://www.youtube.com/watch?v=" + searchResultList.get(0).getId().getVideoId();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
