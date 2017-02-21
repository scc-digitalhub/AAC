package it.smartcommunitylab.aac.profile.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "BasicProfiles")
@XmlAccessorType(XmlAccessType.FIELD)
public class BasicProfiles {

	@XmlElement(name = "BasicProfile")
	private List<BasicProfile> profiles;

	public List<BasicProfile> getProfiles() {
		return profiles;
	}

	public void setProfiles(List<BasicProfile> profiles) {
		this.profiles = profiles;
	}
	
	
	
}
