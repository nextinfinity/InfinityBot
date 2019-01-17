package net.theinfinitymc.infinitybot.commands;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.theinfinitymc.infinitybot.InfinityBot;

import java.io.IOException;
import java.util.List;

public class YouTube implements Command {

	private static com.google.api.services.youtube.YouTube youtube;

	public YouTube(){
		// This object is used to make YouTube Data API requests. The last
		// argument is required, but since we don't need anything
		// initialized when the HttpRequest is initialized, we override
		// the interface and provide a no-op function.
		youtube = new com.google.api.services.youtube.YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {
			public void initialize(HttpRequest request) throws IOException {
			}
		}).setApplicationName("infinitybot-youtube-search").build();
	}

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
			InfinityBot.getAudio().addToQueue(search(query), event.getGuild(), event.getTextChannel(), event.getAuthor());
		}
	}

	@Override
	public String getDescription() {
		return null;
	}

	public String search(String query){

		try {
			// Define the API request for retrieving search results.
			com.google.api.services.youtube.YouTube.Search.List search = youtube.search().list("id,snippet");

			search.setKey("AIzaSyDIrtnnedRFrRqiO-L7bPBqvP_c0KDtrTM");
			search.setQ(query);

			// Restrict the search results to only include videos. See:
			// https://developers.google.com/youtube/v3/docs/search/list#type
			search.setType("video");

			// To increase efficiency, only retrieve the fields that the
			// application uses.
			search.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)");
			search.setMaxResults(1L);

			// Call the API and print results.
			SearchListResponse searchResponse = search.execute();
			List<SearchResult> searchResultList = searchResponse.getItems();

			return "https://www.youtube.com/watch?v=" + searchResultList.get(0).getId().getVideoId();

		} catch (GoogleJsonResponseException e) {
			System.err.println("There was a service error: " + e.getDetails().getCode() + " : "
					+ e.getDetails().getMessage());
		} catch (IOException e) {
			System.err.println("There was an IO error: " + e.getCause() + " : " + e.getMessage());
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return null;
	}

}
