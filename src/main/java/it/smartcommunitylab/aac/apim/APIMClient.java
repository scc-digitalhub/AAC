package it.smartcommunitylab.aac.apim;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import it.smartcommunitylab.aac.model.ClientApp;
import it.smartcommunitylab.aac.oauth.client.OAuth2Client;
import net.minidev.json.JSONObject;

////ignore data from APIM for paramers, we can't properly deserialize it 
//@JsonIgnoreProperties(value = { "parameters" }, allowGetters = true)
public class APIMClient {

    // TODO check with apim
    public static final String SEPARATOR = ",";

    private String clientId;
    private String clientSecret;
    private String clientSecretMobile;
    @NotNull
    private String name;
    private String displayName;
    private String redirectUris;
    private Collection<String> grantedTypes;

    private boolean nativeAppsAccess;
    private Map<String, Map<String, Object>> providerConfigurations;
    private String mobileAppSchema;

    private Map<String, Boolean> identityProviders;
    private Map<String, Boolean> identityProviderApproval;

    @NotNull
    private String userName;
    private String scope;

//    @JsonIgnore
    private Map<String, String> parameters;

    // parameters serialized
    // something like
    // {"tokenScope":"default",
    // "validityPeriod":"3600",
    // "callback_url":null,
    // "grant_types":"refresh_token,password,client_credentials,urn:ietf:params:oauth:grant-type:jwt-bearer",
    // "key_type":"PRODUCTION",
    // "username":"admin"}'
//    private String parameters;

    public APIMClient() {
        this.parameters = Collections.emptyMap();
//        this.parameters = "";
    }

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

//    public String getParameters() {
//        return parameters;
//    }
//
//    public void setParameters(String parameters) {
//        this.parameters = parameters;
//    }

    /*
     * Builders
     */

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public static APIMClient from(OAuth2Client app) {
        APIMClient client = new APIMClient();
        // base
        client.name = app.getName();
        client.userName = app.getRealm();
        client.displayName = app.getDescription();
        // oauth
        client.clientId = app.getClientId();
        client.clientSecret = app.getSecret();
        client.grantedTypes = app.getConfigMap().getAuthorizedGrantTypes()
                .stream().map(gt -> gt.getValue())
                .collect(Collectors.toList());
        client.scope = StringUtils.collectionToDelimitedString(app.getScopes(), SEPARATOR);
        client.redirectUris = StringUtils.collectionToDelimitedString(app.getConfigMap().getRedirectUris(), SEPARATOR);

        // deprecated
        client.clientSecretMobile = "";
        client.nativeAppsAccess = false;
        client.mobileAppSchema = "";

        Map<String, String> parameters = new HashMap<>();
        // apim expects grantTypes in parameters as a string
        parameters.put("grant_types", StringUtils.collectionToDelimitedString(client.grantedTypes, SEPARATOR));
        // apim expects subject as username string in parameters
        // THIS will break since subject != username
        parameters.put("username", app.getClientId());
        // additional parameters
        int validityPeriod = app.getConfigMap().getAccessTokenValidity() != null
                ? app.getConfigMap().getAccessTokenValidity()
                : -1;
        parameters.put("validityPeriod", String.valueOf(validityPeriod));
        parameters.put("tokenScope", client.scope);

//        // lets put also additional information in here even if apim doesn't use it now
//        parameters.put("refreshPeriod", String.valueOf((int) app.getRefreshTokenValidity()));
        client.parameters = parameters;

//        client.parametersMap = parameters;
//        // serialize as JSON for apim to understand values..
//        client.parameters = JSONObject.toJSONString(parameters);
        return client;
    }

}
