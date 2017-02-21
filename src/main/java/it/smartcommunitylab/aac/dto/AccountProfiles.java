package it.smartcommunitylab.aac.dto;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "AccountProfiles")
@XmlAccessorType(XmlAccessType.FIELD)
public class AccountProfiles {

	@XmlElement(name = "AccountProfile")
	private List<AccountProfile> profiles;

	public List<AccountProfile> getProfiles() {
		return profiles;
	}

	public void setProfiles(List<AccountProfile> profiles) {
		this.profiles = profiles;
	}
	
	
	
}
