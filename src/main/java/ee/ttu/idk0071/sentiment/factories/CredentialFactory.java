package ee.ttu.idk0071.sentiment.factories;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ee.ttu.idk0071.sentiment.consts.DomainConsts;
import ee.ttu.idk0071.sentiment.credentials.TwitterCredentials;
import ee.ttu.idk0071.sentiment.lib.fetching.objects.Credentials;
import ee.ttu.idk0071.sentiment.model.Domain;
import ee.ttu.idk0071.sentiment.utils.BeanUtils;
import ee.ttu.idk0071.sentiment.utils.BeanUtils.BeanAccessException;

@Component
public class CredentialFactory {
	@Autowired
	private TwitterCredentials twitterCredentials;

	public Credentials forDomain(Domain domain) {
		if (DomainConsts.DOMAIN_NAME_TWITTER.equals(domain.getName())) {
			Map<String, String> twitterCredentialMap;
			try {
				twitterCredentialMap = BeanUtils.toMap(twitterCredentials);
				return Credentials.from(twitterCredentialMap);
			} catch (BeanAccessException ex) {
				// bad bean
				return null;
			}
		}
		
		return new Credentials();
	}
}
