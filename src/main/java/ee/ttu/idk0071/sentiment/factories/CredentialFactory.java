package ee.ttu.idk0071.sentiment.factories;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ee.ttu.idk0071.sentiment.consts.DomainConsts;
import ee.ttu.idk0071.sentiment.credentials.FourChanCredentialsBean;
import ee.ttu.idk0071.sentiment.credentials.TwitterCredentialsBean;
import ee.ttu.idk0071.sentiment.lib.fetching.objects.Credentials;
import ee.ttu.idk0071.sentiment.model.Domain;
import ee.ttu.idk0071.sentiment.utils.BeanUtils;
import ee.ttu.idk0071.sentiment.utils.BeanUtils.BeanAccessException;

@Component
public class CredentialFactory {
	@Autowired
	private TwitterCredentialsBean twitterCredentials;
	@Autowired
	private FourChanCredentialsBean fourChanCredentials;

	public Credentials forDomain(Domain domain) {
		if (DomainConsts.DOMAIN_CODE_TWITTER.equals(domain.getCode())) {
			return getCredentialsFromBean(twitterCredentials);
		} else if (DomainConsts.DOMAIN_CODE_4CHAN.equals(domain.getCode())) {
			return getCredentialsFromBean(fourChanCredentials);
		}
		
		return new Credentials();
	}

	private static Credentials getCredentialsFromBean(Object bean) {
		Map<String, String> credentialMap;
		try {
			credentialMap = BeanUtils.toMap(bean);
			return Credentials.from(credentialMap);
		} catch (BeanAccessException ex) {
			// bad bean
			return null;
		}
	}
}
