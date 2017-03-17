package ee.ttu.idk0071.sentiment.amqp.messages;

import java.io.Serializable;
import java.util.List;

public class LookupRequestMessage implements Serializable {
	private static final long serialVersionUID = 3203688679494279784L;

	private Long lookupId;
	private List<Long> lookupDomainIds;

	public void setLookupId(Long lookupId) {
		this.lookupId = lookupId;
	}

	public Long getLookupId() {
		return lookupId;
	}

	public void setLookupDomains(List<Long> lookupDomainIds) {
		this.lookupDomainIds = lookupDomainIds;
	}

	public List<Long> getLookupDomains() {
		return lookupDomainIds;
	}
}
