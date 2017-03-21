package ee.ttu.idk0071.sentiment.amqp.messages;

public class DomainLookupRequestMessage {
	private Long domainLookupId;

	public void setDomainLookupId(Long domainLookupId) {
		this.domainLookupId = domainLookupId;
	}

	public Long getDomainLookupId() {
		return domainLookupId;
	}
}
