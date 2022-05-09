package it.smartcommunitylab.aac.webauthn.persistence;

import java.io.Serializable;

import it.smartcommunitylab.aac.SystemKeys;

public class WebAuthnUserAccountId implements Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_WEBAUTHN_SERIAL_VERSION;

    private String provider;
    private String username;

    public WebAuthnUserAccountId() {
    }

    public WebAuthnUserAccountId(String provider, String username) {
        super();
        this.provider = provider;
        this.username = username;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((provider == null) ? 0 : provider.hashCode());
        result = prime * result + ((username == null) ? 0 : username.hashCode());
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
        WebAuthnUserAccountId other = (WebAuthnUserAccountId) obj;
        if (provider == null) {
            if (other.provider != null)
                return false;
        } else if (!provider.equals(other.provider))
            return false;
        if (username == null) {
            if (other.username != null)
                return false;
        } else if (!username.equals(other.username))
            return false;
        return true;
    }

}
