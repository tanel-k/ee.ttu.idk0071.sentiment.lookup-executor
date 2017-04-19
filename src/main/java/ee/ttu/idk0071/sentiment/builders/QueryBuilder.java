package ee.ttu.idk0071.sentiment.builders;

import ee.ttu.idk0071.sentiment.lib.fetching.objects.Credentials;
import ee.ttu.idk0071.sentiment.lib.fetching.objects.Query;

public class QueryBuilder {
	private String keyword;
	private Credentials credentials;
	private long maxResults = 1000L;

	public QueryBuilder setKeyword(String keyword) {
		this.keyword = keyword;
		return this;
	}

	public QueryBuilder setCredentials(Credentials credentials) {
		this.credentials = credentials;
		return this;
	}

	public QueryBuilder setMaxResults(long maxResults) {
		this.maxResults = maxResults;
		return this;
	}

	public Query build() {
		Query query = new Query(keyword, maxResults);
		query.setCredentials(credentials);
		return query;
	}

	public static QueryBuilder builder() {
		QueryBuilder queryBuilder = new QueryBuilder();
		return queryBuilder;
	}
}
