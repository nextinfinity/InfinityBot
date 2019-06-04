package net.theinfinitymc.infinitybot.commands;

import net.dean.jraw.RedditClient;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.SubmissionSearchPaginator;
import net.dean.jraw.paginators.TimePeriod;
import net.dean.jraw.util.Version;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.theinfinitymc.infinitybot.InfinityBot;
import net.theinfinitymc.infinitybot.utils.ArgBuilder;
import net.theinfinitymc.infinitybot.utils.Config;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Reddit implements Command {

	private final RedditClient reddit = new RedditClient(UserAgent.of("desktop",
			"net.theinfinitymc.infinitybot",
			"v" + Version.get().formatted(),
			Config.getRedditUser()));

	@Override
	public void execute(MessageReceivedEvent event, String[] args) {
		if (args.length > 2) {
			String[] data = args[0].split(":");
			String sort = null;
			String time = null;
			Integer amount = null;
			for (String entry : data) {
				if (entry.contains("=")) {
					String[] subdata = entry.split("=");
					if (subdata[0].equalsIgnoreCase("sort")) {
						sort = subdata[1].toUpperCase();
					} else if (subdata[0].equalsIgnoreCase("time")) {
						time = subdata[1].toUpperCase();
					} else if (subdata[0].equalsIgnoreCase("amount")) {
						amount = Integer.valueOf(subdata[1]);
					}
				}
			}
			if (args.length == 3) {
				final String[] search = {sort, time};
				final Integer count = amount;
				InfinityBot.getThreadPool().execute(() ->
						event.getChannel().sendMessage(subredditSearch(args[1].toLowerCase(), search[0], search[1], count)).queue());
			} else {
				String text = ArgBuilder.buildString(Arrays.copyOfRange(args, 2, args.length - 1));
				final String[] search = {text, sort, time};
				final Integer count = amount;
				InfinityBot.getThreadPool().execute(() ->
						event.getChannel().sendMessage(subredditSearch(args[1].toLowerCase(), search[0], search[1], search[2], count)).queue());
			}
		}
	}

	@Override
	public String getDescription() {
		return "Gets a post from the specified subreddit.";
	}

	private String subredditSearch(String sr, String sort, String time, Integer amount) {
		return subredditSearch(sr, null, sort, time, amount);
	}

	private String subredditSearch(String sr, String query, String sort, String time, Integer amount) {
		authenticate();
		SubmissionSearchPaginator sp = new SubmissionSearchPaginator(reddit, query);
		sp.setSubreddit(sr);
		SubmissionSearchPaginator.SearchSort sorting = SubmissionSearchPaginator.SearchSort.RELEVANCE;
		if (sort != null && SubmissionSearchPaginator.SearchSort.valueOf(sort) != null) {
			sorting = SubmissionSearchPaginator.SearchSort.valueOf(sort);
		}
		sp.setSearchSorting(sorting);
		TimePeriod period = TimePeriod.ALL;
		if (time != null && TimePeriod.valueOf(time) != null) {
			period = TimePeriod.valueOf(time);
		}
		sp.setTimePeriod(period);
		Integer limit = 10;
		if (amount != null) {
			limit = amount;
		}
		sp.setLimit(limit);
		List<Listing<Submission>> list = sp.accumulate(2);
		Listing<Submission> listing = list.get(new Random().nextInt(list.size()));
		Submission sub = listing.get(new Random().nextInt(listing.size()));
		return "**" + sub.getTitle() + "**\n" + sub.getUrl();
	}

	private void authenticate() {
		String user = Config.getRedditUser();
		String pass = Config.getRedditPass();
		String id = Config.getRedditId();
		String secret = Config.getRedditSecret();
		Credentials credentials = Credentials.script(user, pass, id, secret);
		OAuthData authData;
		try {
			authData = reddit.getOAuthHelper().easyAuth(credentials);
			reddit.authenticate(authData);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
