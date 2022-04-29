package it.smartcommunitylab.aac.oauth.persistence;

import java.io.Serializable;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.ClientResource;

public abstract class AbstractOAuth2ClientResource implements ClientResource, Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_OAUTH2_SERIAL_VERSION;

    private final String realm;
    protected String clientId;

    protected AbstractOAuth2ClientResource(String realm) {

        this.realm = realm;
    }

    protected AbstractOAuth2ClientResource(String realm, String clientId) {
        this.realm = realm;
        this.clientId = clientId;
    }

    /**
     * Private constructor for JPA and other serialization tools.
     * 
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    private AbstractOAuth2ClientResource() {
        this((String) null, (String) null);
    }

    @Override
    public String getAuthority() {
        return SystemKeys.AUTHORITY_OAUTH2;
    }

    @Override
    public String getRealm() {
        return realm;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    // resource is globally unique and addressable
    // ie given to an external actor he should be able to find the authority and
    // then the provider to request this resource
    @Override
    public String getResourceId() {
        StringBuilder sb = new StringBuilder();
        sb.append(getAuthority()).append(SystemKeys.ID_SEPARATOR);
        sb.append(getId());

        return sb.toString();
    }

    @Override
    public String getUrn() {
        StringBuilder sb = new StringBuilder();
        sb.append(SystemKeys.URN_PROTOCOL).append(SystemKeys.URN_SEPARATOR);
        sb.append(getType()).append(SystemKeys.URN_SEPARATOR);
        sb.append(getResourceId());

        return sb.toString();
    }

    @Override
    public String getUuid() {
        return null;
    }

}