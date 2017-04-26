package ee.ttu.idk0071.sentiment.messaging.executors;

import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.mail.internet.InternetAddress;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

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
import it.ozimov.springboot.mail.model.Email;
import it.ozimov.springboot.mail.model.defaultimpl.DefaultEmail;
import it.ozimov.springboot.mail.service.EmailService;

@Component
public class DomainLookupExecutor {
	@Value("${domain-lookups.max-results}")
	private long maxResults;
	@Value("${lookups.notifications.senderAddress}")
	private String senderAddress;
	@Value("${lookups.notifications.senderName}")
	private String senderName;
	@Value("${lookups.url.lookupDetailBaseURL}")
	private String baseURL;

	@Autowired
	private DomainLookupStateRepository domainLookupStateRepository;
	@Autowired
	private DomainLookupRepository domainLookupRepository;
	@Autowired
	public EmailService emailService;

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

		long neutralCnt = 0, positiveCnt = 0, negativeCnt = 0;

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

		domainLookup.setDomainLookupState(domainLookupStateRepository.findByName(DomainLookupConsts.STATE_COMPLETE));
		domainLookup.setCounts(negativeCnt, neutralCnt, positiveCnt);
		domainLookupRepository.save(domainLookup);
		if (checkIfAllDone(lookup)) sendEmail(domainLookup);
	}

	private void setErrorState(DomainLookup domainLookup) {
		setStateAndSave(domainLookup, DomainLookupConsts.STATE_ERROR);
	}

	private void setStateAndSave(DomainLookup domainLookup, String stateName) {
		domainLookup.setDomainLookupState(domainLookupStateRepository.findByName(stateName));
		domainLookupRepository.save(domainLookup);
		if (checkIfAllDone(domainLookup.getLookup())) sendEmail(domainLookup);
	}

	private Query buildQuery(String queryString, Domain domain) {
		return QueryBuilder.builder().setKeyword(queryString).setMaxResults(maxResults)
				.setCredentials(credentialFactory.forDomain(domain)).build();
	}

	private boolean checkIfAllDone(Lookup lookup) {

		List<DomainLookup> domainLookups = lookup.getDomainLookups();

		for (DomainLookup domainLookup : domainLookups) {
			if (domainLookup.getDomainLookupState().getCode() == DomainLookup.STATE_CODE_IN_PROGRESS
					|| domainLookup.getDomainLookupState().getCode() == DomainLookup.STATE_CODE_QUEUED) {
				return false;
			}
		}
		return true;
	}
	
	private void sendEmail(DomainLookup domainLookup){
		   Lookup lookup = domainLookup.getLookup();
		   Email email = null;
		   String recipient = lookup.getEmail();
		try {
			email = DefaultEmail.builder()
			        .from(new InternetAddress(senderAddress, senderName))
			        .to(Lists.newArrayList(new InternetAddress(recipient,null)))
			        .subject(generateEmailSubject(lookup))
			        .body(generateEmailBody(lookup))
			        .encoding("UTF-8").build();
			
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		   emailService.send(email);
		}
	
	private String generateEmailSubject(Lookup lookup){
		String subject = "Your lookup for " + lookup.getLookupEntity().getName() + " has completed!";
		return subject;
	}
	
	private String generateEmailBody(Lookup lookup){
		String body = 
				"<!doctype html><html><body><p>"
				+ "Hi! <br>"
				+ "we are glad to say that Your lookup for " 
					+ lookup.getLookupEntity().getName() + " has completed! <br>"
				+ "You can see the results if You follow <a href=\"baseURL" 
					+ lookup.getId() + "\">this link.</a><br>"
				+ baseURL + lookup.getId() 
				+ "<br><br> Best regards <br>"
				+ "Sentymental.ly team"
				+ "</p>	</body></html>";
		return body;
	}
	
}
