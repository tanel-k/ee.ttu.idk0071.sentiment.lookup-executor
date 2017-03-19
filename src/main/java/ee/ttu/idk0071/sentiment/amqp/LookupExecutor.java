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
import ee.ttu.idk0071.sentiment.model.SentimentType;
import ee.ttu.idk0071.sentiment.repository.DomainLookupRepository;
import ee.ttu.idk0071.sentiment.repository.LookupRepository;
import ee.ttu.idk0071.sentiment.repository.LookupStateRepository;
import ee.ttu.idk0071.sentiment.repository.SentimentTypeRepository;

@Component
public class LookupExecutor {
	@Autowired
	private LookupRepository lookupRepository;
	@Autowired
	private LookupStateRepository lookupStateRepository;
	@Autowired
	private DomainLookupRepository domainLookupRepository;
	@Autowired
	private SentimentTypeRepository sentimentTypeRepository;

	public void handleMessage(LookupRequestMessage lookupRequest) {
		Lookup lookup = lookupRepository.findOne(lookupRequest.getLookupId());
		LookupEntity lookupEntity = lookup.getLookupEntity();
		
		lookup.setLookupState(lookupStateRepository.findByName("In progress"));
		lookupRepository.save(lookup);
		
		SearchEngineScraper scraper = new GoogleScraper();
		
		String queryString = lookupEntity.getName();
		
		for (DomainLookup domainLookup : lookup.getDomainLookups()) {
			Domain domain = domainLookup.getDomain();
			long neutralCnt = 0, positiveCnt = 0, negativeCnt = 0;
			double neutralQty = 0, positiveQty = 0, negativeQty = 0;
			
			if ("Google".equals(domain.getName())) {
				
				SearchEngineQuery query = new SearchEngineQuery(queryString, 10);
				List<SearchEngineResult> searchLinks = scraper.search(query);
				
				WebsiteAnalyzer analyzer = new WebsiteAnalyzer(500);
				
				for (SearchEngineResult searchLink : searchLinks) {
					try {
						SentimentResult analysisResult = analyzer.analyze(searchLink.getUrl());
						double trustLevel = analysisResult.getTrustLevel();
						
						switch (analysisResult.getSentimentType()) {
							case NEUTRAL:
								neutralCnt++;
								neutralQty += trustLevel;
								break;
							case POSITIVE:
								positiveCnt++;
								positiveQty += trustLevel;
								break;
							case NEGATIVE:
								negativeCnt++;
								negativeQty += trustLevel;
								break;
							default:
								break;
						}
					} catch (Throwable t) {
						continue;
					}
				}
			}
			
			SentimentType lookupSentimentType = null;
			if (neutralCnt >= positiveCnt) {
				if (neutralCnt >= negativeCnt) {
					lookupSentimentType = sentimentTypeRepository.findOne(SentimentType.TYPE_CODE_NEUTRAL);
				} else {
					lookupSentimentType = sentimentTypeRepository.findOne(SentimentType.TYPE_CODE_NEGATIVE);
				}
			} else {
				if (positiveCnt >= negativeCnt) {
					lookupSentimentType = sentimentTypeRepository.findOne(SentimentType.TYPE_CODE_POSITIVE);
				} else {
					lookupSentimentType = sentimentTypeRepository.findOne(SentimentType.TYPE_CODE_NEGATIVE);
				}
			}
			
			domainLookup.setSentimentType(lookupSentimentType);
			domainLookup.setCounts(negativeCnt, neutralCnt, positiveCnt);
			domainLookup.setQuantities(negativeQty, neutralQty, positiveQty);
			domainLookupRepository.save(domainLookup);
		}
		
		lookupRepository.save(lookup);
	}
}
