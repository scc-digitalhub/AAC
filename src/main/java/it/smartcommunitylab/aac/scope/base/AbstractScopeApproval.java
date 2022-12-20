package it.smartcommunitylab.aac.scope.base;

import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonInclude;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.scope.model.ScopeApproval;

public abstract class AbstractScopeApproval implements ScopeApproval {

    @JsonInclude
    @Transient
    private String authority;

    @JsonInclude
    @Transient
    private String provider;

    protected String realm;

    protected String apiResourceId;
    protected String scope;
    protected String subject;
    protected String client;

    protected AbstractScopeApproval(String resourceId, String scope, String subject, String client) {
        this.apiResourceId = resourceId;
        this.scope = scope;

        this.subject = subject;
        this.client = client;
    }

    public final String getType() {
        return SystemKeys.RESOURCE_SCOPE_APPROVAL;
    }

    @Override
    public String getResourceId() {
        StringBuilder sb = new StringBuilder();
        sb.append(subject).append(SystemKeys.ID_SEPARATOR).append(client);
        sb.append(SystemKeys.URN_SEPARATOR).append(scope);
        return sb.toString();
    }

    /**
     * Private constructor for JPA and other serialization tools.
     * 
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    private AbstractScopeApproval() {
        this((String) null, (String) null, (String) null, (String) null);
    }

    // by default resources are associated to repositories, not providers
    // authorityId and provider are transient: implementations should avoid
    // persisting these attributes
    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    @Override
    public String getAuthority() {
        return authority;
    }

    @Override
    public String getProvider() {
        return provider;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getApiResourceId() {
        return apiResourceId;
    }

    public void setApiResourceId(String resourceId) {
        this.apiResourceId = resourceId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

}
