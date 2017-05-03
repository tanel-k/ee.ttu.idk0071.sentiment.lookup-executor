package ee.ttu.idk0071.sentiment.factories;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import ee.ttu.idk0071.sentiment.consts.DomainConsts;
import ee.ttu.idk0071.sentiment.lib.fetching.api.Fetcher;
import ee.ttu.idk0071.sentiment.lib.fetching.impl.BingFetcher;
import ee.ttu.idk0071.sentiment.lib.fetching.impl.DuckDuckGoFetcher;
import ee.ttu.idk0071.sentiment.lib.fetching.impl.FacebookFetcher;
import ee.ttu.idk0071.sentiment.lib.fetching.impl.FourChanFetcher;
import ee.ttu.idk0071.sentiment.lib.fetching.impl.GoogleFetcher;
import ee.ttu.idk0071.sentiment.lib.fetching.impl.RedditFetcher;
import ee.ttu.idk0071.sentiment.lib.fetching.impl.TumblrFetcher;
import ee.ttu.idk0071.sentiment.lib.fetching.impl.TwitterFetcher;
import ee.ttu.idk0071.sentiment.lib.fetching.impl.YahooFetcher;
import ee.ttu.idk0071.sentiment.model.Domain;

@Component
public class FetcherFactory {
	private static final Map<Integer, Constructor<? extends Fetcher>> CONSTRUCTOR_POOL = new HashMap<Integer, Constructor<? extends Fetcher>>();

	static {
		CONSTRUCTOR_POOL.put(DomainConsts.DOMAIN_CODE_GOOGLE, getZeroArgConstructorOrNull(GoogleFetcher.class));
		CONSTRUCTOR_POOL.put(DomainConsts.DOMAIN_CODE_YAHOO, getZeroArgConstructorOrNull(YahooFetcher.class));
		CONSTRUCTOR_POOL.put(DomainConsts.DOMAIN_CODE_BING, getZeroArgConstructorOrNull(BingFetcher.class));
		CONSTRUCTOR_POOL.put(DomainConsts.DOMAIN_CODE_DUCKDUCKGO, getZeroArgConstructorOrNull(DuckDuckGoFetcher.class));
		CONSTRUCTOR_POOL.put(DomainConsts.DOMAIN_CODE_FACEBOOK, getZeroArgConstructorOrNull(FacebookFetcher.class));
		CONSTRUCTOR_POOL.put(DomainConsts.DOMAIN_CODE_TWITTER, getZeroArgConstructorOrNull(TwitterFetcher.class));
		CONSTRUCTOR_POOL.put(DomainConsts.DOMAIN_CODE_TUMBLR, getZeroArgConstructorOrNull(TumblrFetcher.class));
		CONSTRUCTOR_POOL.put(DomainConsts.DOMAIN_CODE_REDDIT, getZeroArgConstructorOrNull(RedditFetcher.class));
		CONSTRUCTOR_POOL.put(DomainConsts.DOMAIN_CODE_4CHAN, getZeroArgConstructorOrNull(FourChanFetcher.class));
	}

	private static <T> Constructor<T> getZeroArgConstructorOrNull(Class<T> clss) {
		try {
			return clss.getConstructor();
		} catch (Throwable t) {
			return null;
		}
	}

	public static class FetcherConstructionException extends Exception {
		private static final long serialVersionUID = 4349717253808181146L;

		public FetcherConstructionException(Throwable t) {
			super(t);
		}
	}

	public Fetcher getFetcher(Domain domain) throws FetcherConstructionException {
		if (CONSTRUCTOR_POOL.containsKey(domain.getCode())) {
			try {
				return CONSTRUCTOR_POOL.get(domain.getCode()).newInstance();
			} catch (Throwable t) {
				throw new FetcherConstructionException(t);
			}
		}
		
		return null;
	}
}
