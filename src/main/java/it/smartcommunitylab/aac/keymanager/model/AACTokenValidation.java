package it.smartcommunitylab.aac.keymanager.model;

public class AACTokenValidation {

	private boolean valid = false;
	private String userId;
	private String username;
	private String clientId;
	
	private long validityPeriod;
	private long issuedTime;
	
	private String[] scope;
	
	private boolean applicationToken;
	
	private String grantType;

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public long getValidityPeriod() {
		return validityPeriod;
	}

	public void setValidityPeriod(long validityPeriod) {
		this.validityPeriod = validityPeriod;
	}

	public long getIssuedTime() {
		return issuedTime;
	}

	public void setIssuedTime(long issuedTime) {
		this.issuedTime = issuedTime;
	}

	public String[] getScope() {
		return scope;
	}

	public void setScope(String[] scope) {
		this.scope = scope;
	}

	public boolean isApplicationToken() {
		return applicationToken;
	}

	public void setApplicationToken(boolean applicationToken) {
		this.applicationToken = applicationToken;
	}

	public String getGrantType() {
		return grantType;
	}

	public void setGrantType(String grantType) {
		this.grantType = grantType;
	}
	
	
	
}
