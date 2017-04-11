package ee.ttu.idk0071.sentiment.messaging.executors;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ee.ttu.idk0071.sentiment.builders.QueryBuilder;
import ee.ttu.idk0071.sentiment.consts.DomainLookupConsts;
import ee.ttu.idk0071.sentiment.factories.AnalyzerFactory;
import ee.ttu.idk0071.sentiment.factories.CredentialFactory;
import ee.ttu.idk0071.sentiment.factories.FetcherFactory;
import ee.ttu.idk0071.sentiment.lib.analysis.api.SentimentAnalyzer;
import ee.ttu.idk0071.sentiment.lib.analysis.api.SentimentRetrievalException;
import ee.ttu.idk0071.sentiment.lib.fetching.api.Fetcher;
import ee.ttu.idk0071.sentiment.lib.fetching.objects.FetchException;
import ee.ttu.idk0071.sentiment.lib.fetching.objects.Query;
import ee.ttu.idk0071.sentiment.lib.messages.DomainLookupRequestMessage;
import ee.ttu.idk0071.sentiment.model.Domain;
import ee.ttu.idk0071.sentiment.model.DomainLookup;
import ee.ttu.idk0071.sentiment.model.Lookup;
import ee.ttu.idk0071.sentiment.model.LookupEntity;
import ee.ttu.idk0071.sentiment.repository.DomainLookupRepository;
import ee.ttu.idk0071.sentiment.repository.DomainLookupStateRepository;

@Component
public class DomainLookupExecutor {
	@Value("${domain-lookups.max-results}")
	private long maxResults; 

	@Autowired
	private DomainLookupStateRepository lookupStateRepository;
	@Autowired
	private DomainLookupRepository domainLookupRepository;

	@Autowired
	private FetcherFactory fetcherFactory;
	@Autowired
	private CredentialFactory credentialFactory;
	@Autowired
	private AnalyzerFactory analyzerFactory;

	public void handleMessage(DomainLookupRequestMessage lookupRequest) throws FetchException {
		DomainLookup domainLookup = domainLookupRepository.findOne(lookupRequest.getDomainLookupId());
		setStateAndSave(domainLookup, DomainLookupConsts.STATE_IN_PROGRESS);
		
		try {
			performLookup(domainLookup);
		} catch (Throwable t) {
			// TODO log error
			setErrorState(domainLookup);
		}
	}

	@Transactional
	public void performLookup(DomainLookup domainLookup) {
		Domain domain = domainLookup.getDomain();
		
		if (!domain.isActive()) {
			setErrorState(domainLookup);
			return;
		}
		
		Lookup lookup = domainLookup.getLookup();
		LookupEntity lookupEntity = lookup.getLookupEntity();
		String queryString = lookupEntity.getName();
		Fetcher fetcher = fetcherFactory.getFetcher(domain);
		
		long neutralCnt = 0, 
			positiveCnt = 0, 
			negativeCnt = 0;
		
		if (fetcher != null) {
			
			Query query = buildQuery(queryString, domain);
			List<String> searchResults;
			try {
				searchResults = fetcher.fetch(query);
			} catch (FetchException ex) {
				// TODO log error
				setErrorState(domainLookup);
				return;
			}
			
			SentimentAnalyzer analyzer = analyzerFactory.getAnalyzer();
			for (String text : searchResults) {
				try {
					switch (analyzer.getSentiment(text)) {
						case NEUTRAL:
							neutralCnt++;
							break;
						case POSITIVE:
							positiveCnt++;
							break;
						case NEGATIVE:
							negativeCnt++;
							break;
						default:
							break;
					}
				} catch (SentimentRetrievalException ex) {
					// TODO log error
					continue;
				}
			}
			
		} else {
			setErrorState(domainLookup);
			return;
		}
		
		domainLookup.setDomainLookupState(lookupStateRepository.findByName(DomainLookupConsts.STATE_COMPLETE));
		domainLookup.setCounts(negativeCnt, neutralCnt, positiveCnt);
		domainLookupRepository.save(domainLookup);
	}

	private void setErrorState(DomainLookup domainLookup) {
		setStateAndSave(domainLookup, DomainLookupConsts.STATE_ERROR);
	}

	private void setStateAndSave(DomainLookup domainLookup, String stateName) {
		domainLookup.setDomainLookupState(lookupStateRepository.findByName(stateName));
		domainLookupRepository.save(domainLookup);
	}

	private Query buildQuery(String queryString, Domain domain) {
		return QueryBuilder.builder()
				.setKeyword(queryString)
				.setMaxResults(maxResults)
				.setCredentials(credentialFactory.forDomain(domain))
				.build();
	}
}
