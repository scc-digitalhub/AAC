package it.smartcommunitylab.aac.saml.persistence;

import java.io.Serializable;

import it.smartcommunitylab.aac.SystemKeys;

public class SamlUserAccountId implements Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_SAML_SERIAL_VERSION;

    private String realm;
    private String provider;
    private String userId;

    public SamlUserAccountId() {
    }

    public SamlUserAccountId(String realm, String provider, String userId) {
        super();
        this.realm = realm;
        this.provider = provider;
        this.userId = userId;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((provider == null) ? 0 : provider.hashCode());
        result = prime * result + ((realm == null) ? 0 : realm.hashCode());
        result = prime * result + ((userId == null) ? 0 : userId.hashCode());
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
        SamlUserAccountId other = (SamlUserAccountId) obj;
        if (provider == null) {
            if (other.provider != null)
                return false;
        } else if (!provider.equals(other.provider))
            return false;
        if (realm == null) {
            if (other.realm != null)
                return false;
        } else if (!realm.equals(other.realm))
            return false;
        if (userId == null) {
            if (other.userId != null)
                return false;
        } else if (!userId.equals(other.userId))
            return false;
        return true;
    }

}