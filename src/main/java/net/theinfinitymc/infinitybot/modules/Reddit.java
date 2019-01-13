package net.theinfinitymc.infinitybot.modules;

import java.util.List;
import java.util.Random;

import net.dean.jraw.RedditClient;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.models.*;
import net.dean.jraw.paginators.Sorting;
import net.dean.jraw.paginators.SubmissionSearchPaginator;
import net.dean.jraw.paginators.SubmissionSearchPaginator.SearchSort;
import net.dean.jraw.paginators.SubredditPaginator;
import net.dean.jraw.paginators.TimePeriod;
import net.dean.jraw.util.Version;

public class Reddit {
	protected final RedditClient reddit = new RedditClient(UserAgent.of("desktop",
            "net.theinfinitymc.infinitybot",
            "v" + Version.get().formatted(),
            "NextInfinityBot"));
	
	public String subredditSearch(String sr, String sort, String time, Integer amount){
		authenticate();
		SubredditPaginator sp = new SubredditPaginator(reddit);
		sp.setSubreddit(sr);
		Sorting sorting = Sorting.HOT;
		if(sort != null && Sorting.valueOf(sort) != null){
			sorting = Sorting.valueOf(sort);
		}
		sp.setSorting(sorting);
		TimePeriod period = TimePeriod.DAY;
		if(time != null && TimePeriod.valueOf(time) != null){
			period = TimePeriod.valueOf(time);
		}
		sp.setTimePeriod(period);
		Integer limit = 10;
		if(amount != null){
			limit = amount;
		}
		sp.setLimit(limit);
		List<Listing<Submission>> list = sp.accumulate(2);
		Listing<Submission> listing = list.get(new Random().nextInt(list.size()));
		Submission sub = listing.get(new Random().nextInt(listing.size()));
		return "**" + sub.getTitle() + "**\n" + sub.getUrl();
	}
	
	public String subredditSearch(String sr, String query, String sort, String time, Integer amount){
		authenticate();
		SubmissionSearchPaginator sp = new SubmissionSearchPaginator(reddit, query);
		sp.setSubreddit(sr);
		SearchSort sorting = SearchSort.RELEVANCE;
		if(sort != null && SearchSort.valueOf(sort) != null){
			sorting = SearchSort.valueOf(sort);
		}
		sp.setSearchSorting(sorting);
		TimePeriod period = TimePeriod.ALL;
		if(time != null && TimePeriod.valueOf(time) != null){
			period = TimePeriod.valueOf(time);
		}
		sp.setTimePeriod(period);
		Integer limit = 10;
		if(amount != null){
			limit = amount;
		}
		sp.setLimit(limit);
		List<Listing<Submission>> list = sp.accumulate(2);
		Listing<Submission> listing = list.get(new Random().nextInt(list.size()));
		Submission sub = listing.get(new Random().nextInt(listing.size()));
		return "**" + sub.getTitle() + "**\n" + sub.getUrl();
	}
	
	public void authenticate(){
		Credentials credentials = Credentials.script("NextInfinityBot", "RNmc0118", "C3_2a1Y8-kRqnA", "OuWrCgqV5LRuuJUtG0izuk-oKBI");
		OAuthData authData;
		try {
			authData = reddit.getOAuthHelper().easyAuth(credentials);
			reddit.authenticate(authData);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
