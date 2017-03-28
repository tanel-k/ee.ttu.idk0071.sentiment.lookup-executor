package ee.ttu.idk0071.sentiment.utils;

import ee.ttu.idk0071.sentiment.lib.fetching.api.Fetcher;
import ee.ttu.idk0071.sentiment.lib.fetching.impl.BingFetcher;
import ee.ttu.idk0071.sentiment.lib.fetching.impl.FacebookFetcher;
import ee.ttu.idk0071.sentiment.lib.fetching.impl.GoogleFetcher;
import ee.ttu.idk0071.sentiment.lib.fetching.impl.TwitterFetcher;
import ee.ttu.idk0071.sentiment.lib.fetching.impl.YahooFetcher;
import ee.ttu.idk0071.sentiment.model.Domain;

public class FetcherFactory {
	private static final String TWITTER = "Twitter";
	private static final String FACEBOOK = "Facebook";
	private static final String BING = "Bing";
	private static final String YAHOO = "Yahoo";
	private static final String GOOGLE = "Google";

	public static Fetcher getFetcher(Domain domain) {
		if (GOOGLE.equals(domain.getName())) {
			return new GoogleFetcher();
		} else if (YAHOO.equals(domain.getName())) {
			return new YahooFetcher();
		} else if (BING.equals(domain.getName())) {
			return new BingFetcher();
		} else if (FACEBOOK.equals(domain.getName())) {
			return new FacebookFetcher();
		} else if (TWITTER.equals(domain.getName())) {
			return new TwitterFetcher();
		} 
		
		return null;
	}
}
