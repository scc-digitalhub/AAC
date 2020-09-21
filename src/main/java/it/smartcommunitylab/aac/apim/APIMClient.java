package it.smartcommunitylab.aac.apim;

import java.util.Collection;
import java.util.Map;
import it.smartcommunitylab.aac.model.ClientAppBasic;

public class APIMClient {

    // TODO check with apim
    public static final String SEPARATOR = ",";

    private String clientId;
    private String clientSecret;
    private String clientSecretMobile;
    private String name;
    private String displayName;
    private String redirectUris;
    private Collection<String> grantedTypes;

    private boolean nativeAppsAccess;
    private Map<String, Map<String, Object>> providerConfigurations;
    private String mobileAppSchema;

    private Map<String, Boolean> identityProviders;
    private Map<String, Boolean> identityProviderApproval;

    private String userName;
    private String scope;

    private String parameters;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getClientSecretMobile() {
        return clientSecretMobile;
    }

    public void setClientSecretMobile(String clientSecretMobile) {
        this.clientSecretMobile = clientSecretMobile;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(String redirectUris) {
        this.redirectUris = redirectUris;
    }

    public Collection<String> getGrantedTypes() {
        return grantedTypes;
    }

    public void setGrantedTypes(Collection<String> grantedTypes) {
        this.grantedTypes = grantedTypes;
    }

    public boolean isNativeAppsAccess() {
        return nativeAppsAccess;
    }

    public void setNativeAppsAccess(boolean nativeAppsAccess) {
        this.nativeAppsAccess = nativeAppsAccess;
    }

    public Map<String, Map<String, Object>> getProviderConfigurations() {
        return providerConfigurations;
    }

    public void setProviderConfigurations(Map<String, Map<String, Object>> providerConfigurations) {
        this.providerConfigurations = providerConfigurations;
    }

    public String getMobileAppSchema() {
        return mobileAppSchema;
    }

    public void setMobileAppSchema(String mobileAppSchema) {
        this.mobileAppSchema = mobileAppSchema;
    }

    public Map<String, Boolean> getIdentityProviders() {
        return identityProviders;
    }

    public void setIdentityProviders(Map<String, Boolean> identityProviders) {
        this.identityProviders = identityProviders;
    }

    public Map<String, Boolean> getIdentityProviderApproval() {
        return identityProviderApproval;
    }

    public void setIdentityProviderApproval(Map<String, Boolean> identityProviderApproval) {
        this.identityProviderApproval = identityProviderApproval;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    /*
     * Builders
     */

    public static APIMClient from(ClientAppBasic app) {
        APIMClient client = new APIMClient();
        // base
        client.name = app.getName();
        client.userName = app.getUserName();
        client.displayName = app.getDisplayName();
        // oauth
        client.clientId = app.getClientId();
        client.clientSecret = app.getClientSecret();
        client.grantedTypes = app.getGrantedTypes();
        client.scope = String.join(SEPARATOR, app.getScope());
        client.redirectUris = String.join(SEPARATOR, app.getRedirectUris());
        // deprecated
        client.clientSecretMobile = "";
        client.nativeAppsAccess = false;
        client.mobileAppSchema = app.getMobileAppSchema();

        return client;
    }

}
