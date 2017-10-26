package it.smartcommunitylab.aac.apimanager.model;

import it.smartcommunitylab.aac.jaxbmodel.Service;

public class ExtendedService extends Service {

	private String apiId;
	private String subscriptionId;

	public String getApiId() {
		return apiId;
	}

	public void setApiId(String apiId) {
		this.apiId = apiId;
	}

	public String getSubscriptionId() {
		return subscriptionId;
	}

	public void setSubscriptionId(String subscriptionId) {
		this.subscriptionId = subscriptionId;
	}


	
	
}
