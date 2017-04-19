package ee.ttu.idk0071.sentiment.factories;

import org.springframework.stereotype.Component;

import ee.ttu.idk0071.sentiment.consts.DomainConsts;
import ee.ttu.idk0071.sentiment.lib.fetching.api.Fetcher;
import ee.ttu.idk0071.sentiment.lib.fetching.impl.BingFetcher;
import ee.ttu.idk0071.sentiment.lib.fetching.impl.FacebookFetcher;
import ee.ttu.idk0071.sentiment.lib.fetching.impl.GoogleFetcher;
import ee.ttu.idk0071.sentiment.lib.fetching.impl.TwitterFetcher;
import ee.ttu.idk0071.sentiment.lib.fetching.impl.YahooFetcher;
import ee.ttu.idk0071.sentiment.model.Domain;

@Component
public class FetcherFactory {
	public Fetcher getFetcher(Domain domain) {
		if (DomainConsts.DOMAIN_NAME_GOOGLE.equals(domain.getName())) {
			return new GoogleFetcher();
		} else if (DomainConsts.DOMAIN_NAME_YAHOO.equals(domain.getName())) {
			return new YahooFetcher();
		} else if (DomainConsts.DOMAIN_NAME_BING.equals(domain.getName())) {
			return new BingFetcher();
		} else if (DomainConsts.DOMAIN_NAME_FACEBOOK.equals(domain.getName())) {
			return new FacebookFetcher();
		} else if (DomainConsts.DOMAIN_NAME_TWITTER.equals(domain.getName())) {
			return new TwitterFetcher();
		} 
		
		return null;
	}
}
