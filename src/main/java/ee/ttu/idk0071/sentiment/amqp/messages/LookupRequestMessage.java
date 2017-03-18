package ee.ttu.idk0071.sentiment.amqp.messages;

public class LookupRequestMessage {
	private Long lookupId;

	public void setLookupId(Long lookupId) {
		this.lookupId = lookupId;
	}

	public Long getLookupId() {
		return lookupId;
	}
}
