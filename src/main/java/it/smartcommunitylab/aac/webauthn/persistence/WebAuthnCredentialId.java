package it.smartcommunitylab.aac.webauthn.persistence;

import java.io.Serializable;

import it.smartcommunitylab.aac.SystemKeys;

public class WebAuthnCredentialId implements Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_WEBAUTHN_SERIAL_VERSION;

    private String provider;
    private String credentialsId;

    public WebAuthnCredentialId() {
    }

    public WebAuthnCredentialId(String provider, String credentialsId) {
        super();
        this.provider = provider;
        this.credentialsId = credentialsId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((credentialsId == null) ? 0 : credentialsId.hashCode());
        result = prime * result + ((provider == null) ? 0 : provider.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        WebAuthnCredentialId other = (WebAuthnCredentialId) obj;
        if (credentialsId == null) {
            if (other.credentialsId != null)
                return false;
        } else if (!credentialsId.equals(other.credentialsId))
            return false;
        if (provider == null) {
            if (other.provider != null)
                return false;
        } else if (!provider.equals(other.provider))
            return false;
        return true;
    }

}
