package ee.ttu.idk0071.sentiment.credentials;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="domains.4chan.credentials")
public class FourChanCredentialsBean {
	private String google4chanCseKey;
	private String googleApiKey;

	public String getGoogle4chanCseKey() {
		return google4chanCseKey;
	}

	public void setGoogle4chanCseKey(String google4chanCseKey) {
		this.google4chanCseKey = google4chanCseKey;
	}

	public String getGoogleApiKey() {
		return googleApiKey;
	}

	public void setGoogleApiKey(String googleApiKey) {
		this.googleApiKey = googleApiKey;
	}
}
