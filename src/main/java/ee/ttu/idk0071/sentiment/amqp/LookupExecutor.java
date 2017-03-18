package ee.ttu.idk0071.sentiment.amqp;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ee.ttu.idk0071.sentiment.amqp.messages.LookupRequestMessage;
import ee.ttu.idk0071.sentiment.lib.analysis.SentimentAnalyzer;
import ee.ttu.idk0071.sentiment.lib.analysis.impl.BasicSentimentAnalyzer;
import ee.ttu.idk0071.sentiment.lib.analysis.objects.PageSentiment;
import ee.ttu.idk0071.sentiment.lib.scraping.SearchEngineScraper;
import ee.ttu.idk0071.sentiment.lib.scraping.impl.GoogleScraper;
import ee.ttu.idk0071.sentiment.lib.scraping.objects.SearchEngineQuery;
import ee.ttu.idk0071.sentiment.lib.scraping.objects.SearchEngineResult;
import ee.ttu.idk0071.sentiment.model.Lookup;
import ee.ttu.idk0071.sentiment.model.LookupDomain;
import ee.ttu.idk0071.sentiment.model.LookupEntity;
import ee.ttu.idk0071.sentiment.model.SentimentSnapshot;
import ee.ttu.idk0071.sentiment.model.SentimentType;
import ee.ttu.idk0071.sentiment.repository.LookupDomainRepository;
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
	private LookupDomainRepository lookupDomainRepository;
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
		SearchEngineQuery query = new SearchEngineQuery(queryString, 10);
		List<SearchEngineResult> searchLinks = scraper.search(query);
		
		float neuCnt = 0, posCnt = 0, negCnt = 0;
		SentimentAnalyzer analyzer = new BasicSentimentAnalyzer(500);
		LookupDomain lookupDomain = lookupDomainRepository.findByName("Google");
		
		for (SearchEngineResult searchLink : searchLinks) {
			try {
				PageSentiment sentiment = analyzer.analyzePage(searchLink.getUrl());
				
				SentimentSnapshot snapshot = new SentimentSnapshot();
				snapshot.setTitle(searchLink.getTitle());
				snapshot.setUrl(searchLink.getUrl());
				snapshot.setTrustLevel(sentiment.getTrustLevel());
				snapshot.setLookup(lookup);
				snapshot.setLookupDomain(lookupDomain);
				snapshot.setDate(new Date());
				
				SentimentType pageSentimentType = null;
				switch (sentiment.getSentimentType()) {
					case NEUTRAL:
						pageSentimentType = sentimentTypeRepository.findOne(SentimentType.TYPE_CODE_NEUTRAL);
						neuCnt += sentiment.getTrustLevel() / 100;
						break;
					case POSITIVE:
						pageSentimentType = sentimentTypeRepository.findOne(SentimentType.TYPE_CODE_POSITIVE);
						posCnt += sentiment.getTrustLevel() / 100;
						break;
					case NEGATIVE:
						pageSentimentType = sentimentTypeRepository.findOne(SentimentType.TYPE_CODE_NEGATIVE);
						negCnt += sentiment.getTrustLevel() / 100;
						break;
					default:
						break;
				}
				
				snapshot.setSentimentType(pageSentimentType);
				snapshot.setLookupDomain(lookupDomain);
				sentimentSnapshotRepository.save(snapshot);
			} catch (Throwable t) {
				continue;
			}
		}
		
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
		
		lookup.setSentimentType(lookupSentimentType);
		lookupRepository.save(lookup);
	}
}
