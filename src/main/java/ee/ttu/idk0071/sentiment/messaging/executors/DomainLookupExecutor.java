package ee.ttu.idk0071.sentiment.messaging.executors;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ee.ttu.idk0071.sentiment.builders.QueryBuilder;
import ee.ttu.idk0071.sentiment.consts.EnvKeys;
import ee.ttu.idk0071.sentiment.factories.AnalyzerFactory;
import ee.ttu.idk0071.sentiment.factories.AnalyzerFactory.NoAvailableAnalyzersException;
import ee.ttu.idk0071.sentiment.factories.CredentialFactory;
import ee.ttu.idk0071.sentiment.factories.FetcherFactory;
import ee.ttu.idk0071.sentiment.factories.FetcherFactory.FetcherConstructionException;
import ee.ttu.idk0071.sentiment.lib.analysis.api.SentimentAnalyzer;
import ee.ttu.idk0071.sentiment.lib.analysis.api.SentimentRetrievalException;
import ee.ttu.idk0071.sentiment.lib.errorHandling.ErrorService;
import ee.ttu.idk0071.sentiment.lib.fetching.api.Fetcher;
import ee.ttu.idk0071.sentiment.lib.fetching.objects.FetchException;
import ee.ttu.idk0071.sentiment.lib.fetching.objects.Query;
import ee.ttu.idk0071.sentiment.lib.messages.DomainLookupRequestMessage;
import ee.ttu.idk0071.sentiment.model.Domain;
import ee.ttu.idk0071.sentiment.model.DomainLookup;
import ee.ttu.idk0071.sentiment.model.DomainLookupState;
import ee.ttu.idk0071.sentiment.model.Lookup;
import ee.ttu.idk0071.sentiment.model.LookupEntity;
import ee.ttu.idk0071.sentiment.repository.DomainLookupRepository;
import ee.ttu.idk0071.sentiment.repository.DomainLookupStateRepository;
import ee.ttu.idk0071.sentiment.services.MailService;
import ee.ttu.idk0071.sentiment.services.objects.Mail;
import ee.ttu.idk0071.sentiment.services.objects.MailParty;

@Component
public class DomainLookupExecutor {
	private static final String COMPLETION_EMAIL_TEMPLATE = "completion-email-template.ftl";
	private static final String CONTEXT_KEY_BASE_URL = "baseURL";
	private static final String CONTEXT_KEY_LOOKUP_ID = "lookupId";
	private static final String CONTEXT_KEY_ENTITY_NAME = "entityName";
	
	private static final String CLASS_NAME = DomainLookupExecutor.class.getName();

	@Value("${domain-lookups.max-results}")
	private long maxResults;
	@Value("${lookups.notifications.sender-address}")
	private String senderAddress;
	@Value("${lookups.notifications.sender-name}")
	private String senderName;

	@Autowired
	private DomainLookupStateRepository domainLookupStateRepository;
	@Autowired
	private DomainLookupRepository domainLookupRepository;

	@Autowired
	public MailService mailService;

	@Autowired
	public ErrorService errorService;
	
	@Autowired
	private FetcherFactory fetcherFactory;
	@Autowired
	private CredentialFactory credentialFactory;
	@Autowired
	private AnalyzerFactory analyzerFactory;

	public void handleMessage(DomainLookupRequestMessage lookupRequest) throws FetchException {
		DomainLookup domainLookup = domainLookupRepository.findOne(lookupRequest.getDomainLookupId());
		setStateAndSave(domainLookup, DomainLookup.STATE_CODE_IN_PROGRESS);
		
		try {
			performLookup(domainLookup);
		} catch (Throwable t) {
			errorService.saveError(t, CLASS_NAME);
			completeLookupWithError(domainLookup);
		}
	}

	@Transactional
	public void performLookup(DomainLookup domainLookup) {
		Domain domain = domainLookup.getDomain();
		
		if (!domain.isActive()) {
			completeLookupWithError(domainLookup);
			return;
		}
		
		Lookup lookup = domainLookup.getLookup();
		LookupEntity lookupEntity = lookup.getLookupEntity();
		String queryString = lookupEntity.getName();
		Fetcher fetcher = null;
		try {
			fetcher = fetcherFactory.getFetcher(domain);
		} catch (FetcherConstructionException e) {
			errorService.saveError(e, CLASS_NAME);
		}
		
		long neutralCnt = 0, positiveCnt = 0, negativeCnt = 0;
		if (fetcher != null) {
			
			Query query = buildQuery(queryString, domain);
			List<String> searchResults;
			try {
				searchResults = fetcher.fetch(query);
			} catch (FetchException ex) {
				errorService.saveError(ex, CLASS_NAME);
				completeLookupWithError(domainLookup);
				return;
			}
			
			SentimentAnalyzer analyzer;
			try {
				analyzer = analyzerFactory.getFirstAvailable();
			} catch (NoAvailableAnalyzersException ex) {
				errorService.saveError(ex, CLASS_NAME);
				completeLookupWithError(domainLookup);
				return;
			}
			
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
					errorService.saveError(ex, CLASS_NAME);
					continue;
				}
			}
			
		} else {
			completeLookupWithError(domainLookup);
			return;
		}
		
		domainLookup.setCounts(negativeCnt, neutralCnt, positiveCnt);
		completeLookup(domainLookup);
	}

	private void terminateWithState(DomainLookup domainLookup, Integer stateCode) {
		setStateAndSave(domainLookup, stateCode);
		if (isParentLookupCompleteFor(domainLookup)) {
			sendCompletionNotification(domainLookup);
		}
	}

	private void completeLookup(DomainLookup domainLookup) {
		domainLookup.setCompletedDate(new Date());
		terminateWithState(domainLookup, DomainLookup.STATE_CODE_COMPLETE);
	}

	private void completeLookupWithError(DomainLookup domainLookup) {
		terminateWithState(domainLookup, DomainLookup.STATE_CODE_ERROR);
	}

	private void setStateAndSave(DomainLookup domainLookup, Integer stateCode) {
		domainLookup.setDomainLookupState(domainLookupStateRepository.findOne(stateCode));
		domainLookupRepository.save(domainLookup);
	}

	private Query buildQuery(String queryString, Domain domain) {
		return QueryBuilder.builder().setKeyword(queryString).setMaxResults(maxResults)
				.setCredentials(credentialFactory.forDomain(domain)).build();
	}

	private DomainLookup refresh(DomainLookup domainLookup) {
		DomainLookup refreshedLookup = domainLookupRepository.findOne(domainLookup.getId());
		return refreshedLookup != null ? refreshedLookup : domainLookup;
	}

	private boolean isParentLookupCompleteFor(DomainLookup domainLookup) {
		domainLookup = refresh(domainLookup);
		return !domainLookup.getLookup().getDomainLookups().stream().anyMatch(dl -> {
			DomainLookupState state = dl.getDomainLookupState();
			return state.getCode() == DomainLookup.STATE_CODE_IN_PROGRESS || state.getCode() == DomainLookup.STATE_CODE_QUEUED;
		});
	}

	private void sendCompletionNotification(DomainLookup domainLookup){
		Lookup lookup = domainLookup.getLookup();
		if (lookup.getEmail() == null) {
			return;
		}
		
		Mail mailModel = constructCompletionNotification(lookup);
		
		Map<String, Object> context = new HashMap<>();
		context.put(CONTEXT_KEY_ENTITY_NAME, lookup.getLookupEntity().getName());
		context.put(CONTEXT_KEY_LOOKUP_ID, String.valueOf(lookup.getId()));
		context.put(CONTEXT_KEY_BASE_URL, System.getenv(EnvKeys.KEY_WEBAPP_URL));
		
		mailService.sendEmailTemplate(mailModel, COMPLETION_EMAIL_TEMPLATE, context);
	}

	private Mail constructCompletionNotification(Lookup lookup) {
		Mail mail = new Mail();
		MailParty recipient = new MailParty();
		recipient.setAddress(lookup.getEmail());
		recipient.setName(null);
		
		MailParty sender = new MailParty();
		sender.setAddress(senderAddress);
		sender.setName(senderName);
		
		mail.setRecipient(recipient);
		mail.setSender(sender);
		
		mail.setSubject("Your lookup for " + lookup.getLookupEntity().getName() + " has completed!");
		return mail;
	}
}
