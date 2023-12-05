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

package it.smartcommunitylab.aac.openidfed.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.openid.connect.sdk.SubjectType;
import com.nimbusds.openid.connect.sdk.federation.registration.ClientRegistrationType;
import com.nimbusds.openid.connect.sdk.rp.ApplicationType;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityProviderConfig;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.identity.provider.IdentityProviderSettingsMap;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.util.StringUtils;

public class OpenIdFedIdentityProviderConfig
    extends AbstractIdentityProviderConfig<OpenIdFedIdentityProviderConfigMap> {

    private static final long serialVersionUID = SystemKeys.AAC_OPENIDFED_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR + OpenIdFedIdentityProviderConfigMap.RESOURCE_TYPE;

    private static final String WELL_KNOWN_CONFIGURATION_OPENID = "/.well-known/openid-configuration";

    //client registrations built for a given op
    private transient Map<String, ClientRegistration> clientRegistrations = new HashMap<>();

    public OpenIdFedIdentityProviderConfig(String provider, String realm) {
        this(SystemKeys.AUTHORITY_OPENIDFED, provider, realm);
    }

    public OpenIdFedIdentityProviderConfig(String authority, String provider, String realm) {
        super(authority, provider, realm, new IdentityProviderSettingsMap(), new OpenIdFedIdentityProviderConfigMap());
    }

    public OpenIdFedIdentityProviderConfig(
        ConfigurableIdentityProvider cp,
        IdentityProviderSettingsMap settingsMap,
        OpenIdFedIdentityProviderConfigMap configMap
    ) {
        super(cp, settingsMap, configMap);
    }

    /**
     * Private constructor for JPA and other serialization tools.
     *
     * We need to implement this to enable deserialization of resources via
     * reflection
     */

    @SuppressWarnings("unused")
    private OpenIdFedIdentityProviderConfig() {
        super();
    }

    public String getRepositoryId() {
        // not configurable, always isolate oidc providers
        return getProvider();
    }

    @JsonIgnore
    public ClientRegistration getClientRegistration(String issuer) {
        return clientRegistrations.computeIfAbsent(issuer, this::toClientRegistration);
    }

    private ClientRegistration toClientRegistration(String issuerUri) {
        if (!StringUtils.hasText(issuerUri)) {
            // unsupported
            return null;
        }

        //autoconf
        // remove well-known path if provided by user
        if (issuerUri.endsWith(WELL_KNOWN_CONFIGURATION_OPENID)) {
            issuerUri = issuerUri.substring(0, issuerUri.length() - WELL_KNOWN_CONFIGURATION_OPENID.length());
        }

        // via builder,
        // providerId is unique, use as registrationId
        ClientRegistration.Builder builder = ClientRegistrations.fromIssuerLocation(issuerUri);

        //we support only automatic registration with private keys for now
        ClientAuthenticationMethod clientAuthenticationMethod = ClientAuthenticationMethod.PRIVATE_KEY_JWT;
        builder.clientAuthenticationMethod(clientAuthenticationMethod);

        String[] scope = StringUtils.commaDelimitedListToStringArray(configMap.getScope());
        builder.scope(scope);

        builder.userNameAttributeName(configMap.getUserNameAttributeName());

        // we support only authCode
        builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
        // add our redirect template
        builder.redirectUri(getRedirectUrl());

        // set client
        builder.clientId(configMap.getClientId());
        builder.clientName(configMap.getClientName());

        // re-set registrationId since auto-configuration sets values provided from
        // issuer
        builder.registrationId(getProvider());

        return builder.build();
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

    public JWK getClientJWK() {
        if (!StringUtils.hasText(configMap.getClientJwks())) {
            return null;
        }

        // expect a single key as jwk
        try {
            return JWK.parse(configMap.getClientJwks());
        } catch (ParseException e) {
            return null;
        }
    }

    public JWK getFederationJWK() {
        if (!StringUtils.hasText(configMap.getFederationJwks())) {
            //fall back to client keys
            return getClientJWK();
        }

        // expect a single key as jwk
        try {
            return JWK.parse(configMap.getFederationJwks());
        } catch (ParseException e) {
            return null;
        }
    }

    public SubjectType getSubjectType() {
        return configMap.getSubjectType() != null ? configMap.getSubjectType() : SubjectType.PAIRWISE;
    }

    /**
     * Configuration details, set by default
     */

    public com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod getClientAuthenticationMethod() {
        //we support only private key
        return com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod.PRIVATE_KEY_JWT;
    }

    public ClientRegistrationType getClientRegistrationType() {
        //we support only automatic
        return ClientRegistrationType.AUTOMATIC;
    }

    public ApplicationType getApplicationType() {
        //we support only web
        return ApplicationType.WEB;
    }

    public List<GrantType> getGrantTypes() {
        //always auth code
        //TODO add support for refresh
        return Collections.singletonList(GrantType.AUTHORIZATION_CODE);
    }

    public List<ResponseType> getResponseTypes() {
        //only code
        //TODO support code+idtoken via config, should switch user service from userinfo to idtoken
        return Collections.singletonList(ResponseType.CODE);
    }

    public String getRedirectUrl() {
        return "{baseUrl}/auth/" + getAuthority() + "/{action}/{registrationId}";
    }
}
