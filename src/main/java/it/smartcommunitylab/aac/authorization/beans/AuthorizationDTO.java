package it.smartcommunitylab.aac.authorization.beans;

public class AuthorizationDTO {
	private String id;
	private AuthorizationUserDTO subject;
	private String action;
	private AuthorizationResourceDTO resource;
	private AuthorizationUserDTO entity;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public AuthorizationUserDTO getSubject() {
		return subject;
	}

	public void setSubject(AuthorizationUserDTO subject) {
		this.subject = subject;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public AuthorizationResourceDTO getResource() {
		return resource;
	}

	public void setResource(AuthorizationResourceDTO resource) {
		this.resource = resource;
	}

	public AuthorizationUserDTO getEntity() {
		return entity;
	}

	public void setEntity(AuthorizationUserDTO entity) {
		this.entity = entity;
	}

}
