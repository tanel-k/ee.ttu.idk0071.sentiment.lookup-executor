package ee.ttu.idk0071.sentiment.messaging.executors;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.boot.test.context.SpringBootTest;

import ee.ttu.idk0071.sentiment.factories.AnalyzerFactory;
import ee.ttu.idk0071.sentiment.factories.CredentialFactory;
import ee.ttu.idk0071.sentiment.factories.FetcherFactory;
import ee.ttu.idk0071.sentiment.lib.analysis.api.SentimentAnalyzer;
import ee.ttu.idk0071.sentiment.lib.analysis.objects.SentimentType;
import ee.ttu.idk0071.sentiment.lib.fetching.api.Fetcher;
import ee.ttu.idk0071.sentiment.lib.fetching.objects.Credentials;
import ee.ttu.idk0071.sentiment.lib.fetching.objects.FetchException;
import ee.ttu.idk0071.sentiment.lib.fetching.objects.Query;
import ee.ttu.idk0071.sentiment.model.Domain;
import ee.ttu.idk0071.sentiment.model.DomainLookup;
import ee.ttu.idk0071.sentiment.model.DomainLookupState;
import ee.ttu.idk0071.sentiment.model.Lookup;
import ee.ttu.idk0071.sentiment.model.LookupEntity;
import ee.ttu.idk0071.sentiment.repository.DomainLookupRepository;
import ee.ttu.idk0071.sentiment.repository.DomainLookupStateRepository;
import ee.ttu.idk0071.sentiment.services.MailService;
import ee.ttu.idk0071.sentiment.services.objects.Mail;

@SpringBootTest
@RunWith(PowerMockRunner.class)
@PrepareForTest(DomainLookupExecutor.class)
public class DomainLookupExecutorTests {
	@InjectMocks
	private DomainLookupExecutor domainLookupExecutor = new DomainLookupExecutor();

	@Mock
	private DomainLookupStateRepository lookupStateRepository;
	@Mock
	private DomainLookupRepository domainLookupRepository;
	@Mock
	private FetcherFactory fetcherFactory;
	@Mock
	private CredentialFactory credentialFactory;
	@Mock
	private AnalyzerFactory analyzerFactory;
	@Mock
	private MailService mailService;

	private DomainLookupState errorState;
	private DomainLookupState completeState;

	@Before
	@SuppressWarnings("unchecked")
	public void beforeTests() {
		Mockito.doNothing().when(mailService).sendEmailTemplate(Mockito.any(Mail.class), Mockito.any(String.class), Mockito.any(Map.class));
		errorState = new DomainLookupState();
		Mockito.when(lookupStateRepository.findOne(DomainLookup.STATE_CODE_ERROR)).thenReturn(errorState);
		completeState = new DomainLookupState();
		Mockito.when(lookupStateRepository.findOne(DomainLookup.STATE_CODE_COMPLETE)).thenReturn(completeState);
	}

	@Test
	public void testInactiveDomainCausesErrorState() {
		Domain inactiveDomain = new Domain();
		inactiveDomain.setActive(false);
		Lookup lookup = new Lookup();
		DomainLookup domainLookup = new DomainLookup();
		domainLookup.setLookup(lookup);
		lookup.getDomainLookups().add(domainLookup);
		domainLookup.setDomain(inactiveDomain);
		
		domainLookupExecutor.performLookup(domainLookup);
		Assert.assertEquals(domainLookup.getDomainLookupState(), errorState);
	}

	@Test(expected=RuntimeException.class)
	public void testThrowableCausesErrorstate() {
		DomainLookup domainLookup = new DomainLookup();
		Mockito.doThrow(new RuntimeException()).when(domainLookupExecutor).performLookup(domainLookup);
		
		domainLookupExecutor.performLookup(domainLookup);
		Assert.assertEquals(domainLookup.getDomainLookupState(), errorState);
	}

	@Test
	public void testNoFetcherCausesErrorState() throws Exception {
		DomainLookup domainLookup = getMockDomainLookup();
		Mockito.when(fetcherFactory.getFetcher(domainLookup.getDomain())).thenReturn(null);
		
		domainLookupExecutor.performLookup(domainLookup);
		Assert.assertEquals(domainLookup.getDomainLookupState(), errorState);
	}

	@Test
	public void testFetchExceptionCausesErrorState() throws Exception {
		DomainLookup domainLookup = getMockDomainLookup();
		Fetcher fetcher = Mockito.mock(Fetcher.class);
		
		Mockito.when(fetcher.fetch(Mockito.any(Query.class))).thenThrow(new FetchException());
		Mockito.when(fetcherFactory.getFetcher(domainLookup.getDomain())).thenReturn(fetcher);
		
		domainLookupExecutor.performLookup(domainLookup);
		Assert.assertEquals(domainLookup.getDomainLookupState(), errorState);
	}

	@Test
	public void testSentimentCountedCorrectly() throws Exception {
		DomainLookup domainLookup = getMockDomainLookup();
		Fetcher fetcher = Mockito.mock(Fetcher.class);
		SentimentAnalyzer sentimentAnalyzer = Mockito.mock(SentimentAnalyzer.class);
		String textResult = "";
		Credentials credentials = Mockito.mock(Credentials.class);
		
		List<String> fetchResults = new LinkedList<>();
		List<SentimentType> sentiments = new LinkedList<>();
		int negativeCount = 0, neutralCount = 0, positiveCount = 0;
		for (int i = 0; i < 1000; i++) {
			fetchResults.add(textResult);
			SentimentType fetchResultSentiment = getRandomSentiment();
			
			switch (fetchResultSentiment) {
				case NEGATIVE:
					negativeCount++;
					break;
				case NEUTRAL:
					neutralCount++;
					break;
				case POSITIVE:
					positiveCount++;
					break;
				default:
					break;
			}
			
			sentiments.add(fetchResultSentiment);
		}
		
		Mockito.when(fetcherFactory.getFetcher(domainLookup.getDomain())).thenReturn(fetcher);
		Mockito.when(fetcher.fetch(Mockito.any(Query.class))).thenReturn(fetchResults);
		Mockito.when(analyzerFactory.getFirstAvailable()).thenReturn(sentimentAnalyzer);
		Mockito.when(credentialFactory.forDomain(Mockito.any(Domain.class))).thenReturn(credentials);
		
		SentimentType firstSentiment = sentiments.get(0);
		SentimentType[] otherSentiments = sentiments.subList(1, sentiments.size()).toArray(new SentimentType[sentiments.size() - 1]);
		Mockito.when(sentimentAnalyzer.getSentiment(Mockito.anyString())).thenReturn(firstSentiment, otherSentiments);
		
		domainLookupExecutor.performLookup(domainLookup);
		
		Assert.assertEquals(domainLookup.getDomainLookupState(), completeState);
		Assert.assertEquals(domainLookup.getNegativeCount(), negativeCount, 0.0);
		Assert.assertEquals(domainLookup.getNeutralCount(), neutralCount, 0.0);
		Assert.assertEquals(domainLookup.getPositiveCount(), positiveCount, 0.0);
	}

	public SentimentType getRandomSentiment() {
		int randIndex = ThreadLocalRandom.current().nextInt(0, 3);
		return SentimentType.values()[randIndex];
	}

	public DomainLookup getMockDomainLookup() {
		Domain domain = new Domain();
		domain.setActive(true);
		
		LookupEntity lookupEntity = new LookupEntity();
		lookupEntity.setName("testname");
		
		Lookup lookup = new Lookup();
		lookup.setLookupEntity(lookupEntity);
		
		DomainLookup domainLookup = new DomainLookup();
		domainLookup.setLookup(lookup);
		domainLookup.setDomain(domain);
		
		lookup.getDomainLookups().add(domainLookup);
		
		return domainLookup;
	}
}
