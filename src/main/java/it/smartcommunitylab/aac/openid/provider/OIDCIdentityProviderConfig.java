/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.openid.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jose.jwk.JWK;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.provider.config.AbstractIdentityProviderConfig;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.oauth.model.AuthenticationMethod;
import java.text.ParseException;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.util.StringUtils;

public class OIDCIdentityProviderConfig extends AbstractIdentityProviderConfig<OIDCIdentityProviderConfigMap> {

    private static final long serialVersionUID = SystemKeys.AAC_OIDC_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR + OIDCIdentityProviderConfigMap.RESOURCE_TYPE;

    private static final String WELL_KNOWN_CONFIGURATION_OPENID = "/.well-known/openid-configuration";

    private transient ClientRegistration clientRegistration;

    public OIDCIdentityProviderConfig(String provider, String realm) {
        this(SystemKeys.AUTHORITY_OIDC, provider, realm);
    }

    public OIDCIdentityProviderConfig(
        @JsonProperty("authority") String authority,
        @JsonProperty("provider") String provider,
        @JsonProperty("realm") String realm
    ) {
        super(authority, provider, realm, new OIDCIdentityProviderConfigMap());
        this.clientRegistration = null;
    }

    public OIDCIdentityProviderConfig(ConfigurableIdentityProvider cp, OIDCIdentityProviderConfigMap configMap) {
        super(cp, configMap);
    }

    public String getRepositoryId() {
        // not configurable, always isolate oidc providers
        return getProvider();
    }

    @JsonIgnore
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
            String issuerUri = configMap.getIssuerUri();
            // remove well-known path if provided by user
            if (issuerUri.endsWith(WELL_KNOWN_CONFIGURATION_OPENID)) {
                issuerUri = issuerUri.substring(0, issuerUri.length() - WELL_KNOWN_CONFIGURATION_OPENID.length());
            }

            builder = ClientRegistrations.fromIssuerLocation(issuerUri);
        }

        // set config
        ClientAuthenticationMethod clientAuthenticationMethod = ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
        if (configMap.getClientAuthenticationMethod() != null) {
            // we support only these methods
            if (AuthenticationMethod.CLIENT_SECRET_POST == configMap.getClientAuthenticationMethod()) {
                clientAuthenticationMethod = ClientAuthenticationMethod.CLIENT_SECRET_POST;
            } else if (AuthenticationMethod.NONE == configMap.getClientAuthenticationMethod()) {
                clientAuthenticationMethod = ClientAuthenticationMethod.NONE;
            } else if (AuthenticationMethod.CLIENT_SECRET_JWT == configMap.getClientAuthenticationMethod()) {
                clientAuthenticationMethod = ClientAuthenticationMethod.CLIENT_SECRET_JWT;
            } else if (AuthenticationMethod.PRIVATE_KEY_JWT == configMap.getClientAuthenticationMethod()) {
                clientAuthenticationMethod = ClientAuthenticationMethod.PRIVATE_KEY_JWT;
            }
        }
        builder.clientAuthenticationMethod(clientAuthenticationMethod);

        String[] scope = StringUtils.commaDelimitedListToStringArray(configMap.getScope());
        builder.scope(scope);

        builder.userNameAttributeName(configMap.getUserNameAttributeName());

        // we support only authCode
        // TODO check PKCE
        builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
        // add our redirect template
        builder.redirectUri(getRedirectUrl());

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

    public String getSubAttributeName() {
        return StringUtils.hasText(configMap.getSubAttributeName())
            ? configMap.getSubAttributeName()
            : IdTokenClaimNames.SUB;
    }

    public boolean trustEmailAddress() {
        // do not trust email by default
        return configMap.getTrustEmailAddress() != null ? configMap.getTrustEmailAddress().booleanValue() : false;
    }

    public boolean alwaysTrustEmailAddress() {
        // do not trust email by default
        return configMap.getAlwaysTrustEmailAddress() != null
            ? configMap.getAlwaysTrustEmailAddress().booleanValue()
            : false;
    }

    public boolean requireEmailAddress() {
        return configMap.getRequireEmailAddress() != null ? configMap.getRequireEmailAddress().booleanValue() : false;
    }

    public ClientAuthenticationMethod getClientAuthenticationMethod() {
        // TODO Auto-generated method stub
        return getClientRegistration().getClientAuthenticationMethod();
    }

    public boolean isPkceEnabled() {
        return configMap.getEnablePkce() != null ? configMap.getEnablePkce().booleanValue() : true;
    }

    public JWK getClientJWK() {
        if (!StringUtils.hasText(configMap.getClientJwk())) {
            return null;
        }

        // expect a single key as jwk
        try {
            return JWK.parse(configMap.getClientJwk());
        } catch (ParseException e) {
            return null;
        }
    }

    /*
     * builders
     */
    //    public static ConfigurableIdentityProvider toConfigurableProvider(OIDCIdentityProviderConfig op) {
    //        ConfigurableIdentityProvider cp = new ConfigurableIdentityProvider(SystemKeys.AUTHORITY_OIDC, op.getProvider(),
    //                op.getRealm());
    //        cp.setType(SystemKeys.RESOURCE_IDENTITY);
    //        cp.setPersistence(op.getPersistence());
    //
    //        cp.setName(op.getName());
    //        cp.setDescription(op.getDescription());
    //        cp.setHookFunctions(op.getHookFunctions());
    //
    //        cp.setEnabled(true);
    //        cp.setLinkable(op.isLinkable());
    //        cp.setConfiguration(op.getConfiguration());
    //
    //        return cp;
    //    }
    //
    //    public static OIDCIdentityProviderConfig fromConfigurableProvider(ConfigurableIdentityProvider cp) {
    //        OIDCIdentityProviderConfig op = new OIDCIdentityProviderConfig(cp.getAuthority(), cp.getProvider(),
    //                cp.getRealm());
    //        op.configMap = new OIDCIdentityProviderConfigMap();
    //        op.configMap.setConfiguration(cp.getConfiguration());
    //
    //        op.name = cp.getName();
    //        op.description = cp.getDescription();
    //        op.icon = cp.getIcon();
    //
    //        op.linkable = cp.isLinkable();
    //        op.persistence = cp.getPersistence();
    //        op.events = cp.getEvents();
    //        op.position = cp.getPosition();
    //
    //        op.hookFunctions = (cp.getHookFunctions() != null ? cp.getHookFunctions() : Collections.emptyMap());
    //
    //        return op;
    //    }

    public String getRedirectUrl() {
        return "{baseUrl}/auth/" + getAuthority() + "/{action}/{registrationId}";
    }
}
