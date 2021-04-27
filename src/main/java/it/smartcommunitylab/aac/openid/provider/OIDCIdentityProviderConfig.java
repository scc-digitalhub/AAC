package it.smartcommunitylab.aac.openid.provider;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractConfigurableProvider;
import it.smartcommunitylab.aac.core.base.ConfigurableProvider;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfigMap;
import it.smartcommunitylab.aac.openid.OIDCIdentityAuthority;

public class OIDCIdentityProviderConfig extends AbstractConfigurableProvider {

    public static final String PROVIDER_GOOGLE = "google";
    public static final String PROVIDER_FACEBOOK = "facebook";
    public static final String PROVIDER_GITHUB = "github";

    public static final String DEFAULT_REDIRECT_URL = "{baseUrl}" + OIDCIdentityAuthority.AUTHORITY_URL
            + "{action}/{registrationId}";

    private static ObjectMapper mapper = new ObjectMapper();
    private final static TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
    };

    private String name;
    private String persistence;

    private OIDCIdentityProviderConfigMap configMap;
    private ClientRegistration clientRegistration;

    public OIDCIdentityProviderConfig(String provider, String realm) {
        super(SystemKeys.AUTHORITY_OIDC, provider, realm);
        this.clientRegistration = null;
        this.configMap = new OIDCIdentityProviderConfigMap();
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public OIDCIdentityProviderConfigMap getConfigMap() {
        return configMap;
    }

    public void setConfigMap(OIDCIdentityProviderConfigMap configMap) {
        this.configMap = configMap;
    }

    public String getPersistence() {
        return persistence;
    }

    public void setPersistence(String persistence) {
        this.persistence = persistence;
    }

    @Override
    public Map<String, Object> getConfiguration() {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        return mapper.convertValue(configMap, typeRef);
    }

    public ClientRegistration getClientRegistration() {
        if (clientRegistration == null) {
            clientRegistration = toClientRegistration();
        }

        return clientRegistration;
    }

    // TODO map attributes? we could simply use clientRegistation
    // TODO validate? we could simply use clientRegistation

    private ClientRegistration toClientRegistration() {
//        // read base params from configMap
//        String clientId = (String) getConfigurationProperty("clientId");
//        String clientSecret = (String) getConfigurationProperty("clientSecret");
//        String clientName = (String) getConfigurationProperty("clientName");
//
//        // read default params from configMap
//        String clientAuthenticationMethod = getProperty("clientAuthenticationMethod",
//                ClientAuthenticationMethod.BASIC.getValue());
//
//        String[] scope = StringUtils.commaDelimitedListToStringArray(getProperty("scope", "openid,profile,email"));
//
//        String userNameAttributeName = getProperty("userNameAttributeName", "sub");
//
//        // read provider params
//        // explicit config
//        String authorizationUri = (String) getConfigurationProperty("authorizationUri");
//        String tokenUri = (String) getConfigurationProperty("tokenUri");
//        String jwkSetUri = (String) getConfigurationProperty("jwkSetUri");
//        String userInfoUri = (String) getConfigurationProperty("userInfoUri");
//
//        // autoconfiguration support from well-known
//        String issuerUri = (String) getConfigurationProperty("issuerUri");

        // via builder,
        // providerId is unique, use as registrationId
        ClientRegistration.Builder builder = ClientRegistration.withRegistrationId(getProvider());
//        // get templates if naming matches
//        ClientRegistration.Builder template = getCommonBuilder(getProvider(), clientName);
//        if (template != null) {
//            // use template
//            builder = template;
//        }

        // check for autoconf, will override template
        if (StringUtils.hasText(configMap.getIssuerUri())) {
            builder = ClientRegistrations.fromIssuerLocation(configMap.getIssuerUri());
        }

        // set config
        builder.clientAuthenticationMethod(configMap.getClientAuthenticationMethod());

        String[] scope = StringUtils.commaDelimitedListToStringArray(configMap.getScope());
        builder.scope(scope);

        builder.userNameAttributeName(configMap.getUserNameAttributeName());

        // we support only authCode
        // TODO check PKCE
        builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
        // add our redirect template
        builder.redirectUri(DEFAULT_REDIRECT_URL);

        // set client
        builder.clientId(configMap.getClientId());
        builder.clientSecret(configMap.getClientSecret());
        builder.clientName(configMap.getClientName());

        // set only non null
        if (StringUtils.hasText(configMap.getAuthorizationUri())) {
            builder.authorizationUri(configMap.getAuthorizationUri());
        }

        if (StringUtils.hasText(configMap.getTokenUri())) {
            builder.tokenUri(configMap.getTokenUri());
        }

        if (StringUtils.hasText(configMap.getUserInfoUri())) {
            builder.userInfoUri(configMap.getUserInfoUri());
        }

        if (StringUtils.hasText(configMap.getJwkSetUri())) {
            builder.jwkSetUri(configMap.getJwkSetUri());
        }

        // DISABLED, need spring security next version
//        if (StringUtils.hasText(issuerUri)) {
//            builder.issuerUri(issuerUri);
//        }

        // re-set registrationId since auto-configuration sets values provided from
        // issuer
        builder.registrationId(getProvider());

        return builder.build();

    }

//    public static ClientRegistration.Builder getCommonBuilder(String registrationId, String name) {
//        ClientRegistration.Builder builder = null;
//        if (PROVIDER_GOOGLE.equals(name)) {
//            builder = CommonOAuth2Provider.GOOGLE.getBuilder(registrationId);
//        } else if (PROVIDER_FACEBOOK.equals(name)) {
//            builder = CommonOAuth2Provider.FACEBOOK.getBuilder(registrationId);
//        } else if (PROVIDER_GITHUB.equals(name)) {
//            builder = CommonOAuth2Provider.GITHUB.getBuilder(registrationId);
//        }
//
//        // TODO add additional templates (and read from config!)
//
//        return builder;
//
//    }

//    private String getProperty(String key, String defaultValue) {
//        if (StringUtils.hasText((String) getConfigurationProperty(key))) {
//            return (String) getConfigurationProperty(key);
//        }
//
//        return defaultValue;
//    }

    /*
     * builders
     */
    public static ConfigurableProvider toConfigurableProvider(OIDCIdentityProviderConfig op) {
        ConfigurableProvider cp = new ConfigurableProvider(SystemKeys.AUTHORITY_OIDC, op.getProvider(), op.getRealm());
        cp.setType(SystemKeys.RESOURCE_IDENTITY);
        cp.setConfiguration(op.getConfiguration());
        cp.setName(op.getName());

        return cp;
    }

    public static OIDCIdentityProviderConfig fromConfigurableProvider(ConfigurableProvider cp) {
        OIDCIdentityProviderConfig op = new OIDCIdentityProviderConfig(cp.getProvider(), cp.getRealm());
//        op.setConfiguration(cp.getConfiguration());
        op.configMap = mapper.convertValue(cp.getConfiguration(), OIDCIdentityProviderConfigMap.class);
        op.name = cp.getName();
        return op;

    }

}
