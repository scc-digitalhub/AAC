package it.smartcommunitylab.aac.keymanager.model;

import java.util.List;

import com.google.common.collect.Lists;


public class AACService {

	private String serviceName;
	private String description;
	
	private List<AACResource> resources;
	
	public AACService() {
		resources = Lists.newArrayList();
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<AACResource> getResources() {
		return resources;
	}

	public void setResources(List<AACResource> resources) {
		this.resources = resources;
	}

	
}
