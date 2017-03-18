package ee.ttu.idk0071.sentiment.amqp;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ee.ttu.idk0071.sentiment.amqp.messages.LookupRequestMessage;
import ee.ttu.idk0071.sentiment.lib.analysis.impl.WebsiteAnalyzer;
import ee.ttu.idk0071.sentiment.lib.analysis.objects.SentimentResult;
import ee.ttu.idk0071.sentiment.lib.scraping.SearchEngineScraper;
import ee.ttu.idk0071.sentiment.lib.scraping.impl.GoogleScraper;
import ee.ttu.idk0071.sentiment.lib.scraping.objects.SearchEngineQuery;
import ee.ttu.idk0071.sentiment.lib.scraping.objects.SearchEngineResult;
import ee.ttu.idk0071.sentiment.model.Domain;
import ee.ttu.idk0071.sentiment.model.DomainLookup;
import ee.ttu.idk0071.sentiment.model.Lookup;
import ee.ttu.idk0071.sentiment.model.LookupEntity;
import ee.ttu.idk0071.sentiment.model.SentimentSnapshot;
import ee.ttu.idk0071.sentiment.model.SentimentType;
import ee.ttu.idk0071.sentiment.repository.LookupRepository;
import ee.ttu.idk0071.sentiment.repository.LookupStateRepository;
import ee.ttu.idk0071.sentiment.repository.SentimentSnapshotRepository;
import ee.ttu.idk0071.sentiment.repository.SentimentTypeRepository;

@Component
public class LookupExecutor {
	@Autowired
	private LookupRepository lookupRepository;
	@Autowired
	private LookupStateRepository lookupStateRepository;
	@Autowired
	private SentimentTypeRepository sentimentTypeRepository;
	@Autowired
	private SentimentSnapshotRepository sentimentSnapshotRepository;

	public void handleMessage(LookupRequestMessage lookupRequest) {
		Lookup lookup = lookupRepository.findOne(lookupRequest.getLookupId());
		LookupEntity lookupEntity = lookup.getLookupEntity();
		
		lookup.setLookupState(lookupStateRepository.findByName("In progress"));
		lookupRepository.save(lookup);
		
		SearchEngineScraper scraper = new GoogleScraper();
		
		String queryString = lookupEntity.getName();
		
		for (DomainLookup domainLookup : lookup.getDomainLookups()) {
			Domain domain = domainLookup.getDomain();
			if ("Google".equals(domain.getName())) {
				
				SearchEngineQuery query = new SearchEngineQuery(queryString, 10);
				List<SearchEngineResult> searchLinks = scraper.search(query);
				
				WebsiteAnalyzer analyzer = new WebsiteAnalyzer(500);
				
				for (SearchEngineResult searchLink : searchLinks) {
					try {
						SentimentResult sentiment = analyzer.analyze(searchLink.getUrl());
						
						SentimentSnapshot snapshot = new SentimentSnapshot();
						snapshot.setSource(searchLink.getUrl());
						snapshot.setTrustLevel(sentiment.getTrustLevel());
						snapshot.setDomainLookup(domainLookup);
						
						SentimentType pageSentimentType = null;
						switch (sentiment.getSentimentType()) {
							case NEUTRAL:
								pageSentimentType = sentimentTypeRepository.findOne(SentimentType.TYPE_CODE_NEUTRAL);
								break;
							case POSITIVE:
								pageSentimentType = sentimentTypeRepository.findOne(SentimentType.TYPE_CODE_POSITIVE);
								break;
							case NEGATIVE:
								pageSentimentType = sentimentTypeRepository.findOne(SentimentType.TYPE_CODE_NEGATIVE);
								break;
							default:
								break;
						}
						
						snapshot.setSentimentType(pageSentimentType);
						sentimentSnapshotRepository.save(snapshot);
					} catch (Throwable t) {
						continue;
					}
				}
				
				/*
				SentimentType lookupSentimentType = null;
				if (neuCnt >= posCnt) {
					if (neuCnt >= negCnt) {
						lookupSentimentType = sentimentTypeRepository.findOne(SentimentType.TYPE_CODE_NEUTRAL);
					} else {
						lookupSentimentType = sentimentTypeRepository.findOne(SentimentType.TYPE_CODE_NEGATIVE);
					}
				} else {
					if (posCnt >= negCnt) {
						lookupSentimentType = sentimentTypeRepository.findOne(SentimentType.TYPE_CODE_POSITIVE);
					} else {
						lookupSentimentType = sentimentTypeRepository.findOne(SentimentType.TYPE_CODE_NEGATIVE);
					}
				}
				*/
				
				lookupRepository.save(lookup);
			}
		}
	}
}
