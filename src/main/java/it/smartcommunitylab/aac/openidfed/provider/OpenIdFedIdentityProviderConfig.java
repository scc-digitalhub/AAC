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
import it.smartcommunitylab.aac.openidfed.auth.OpenIdFedClientRegistrationRepository;
import it.smartcommunitylab.aac.openidfed.service.ListingOpenIdProviderDiscoveryService;
import it.smartcommunitylab.aac.openidfed.service.OpenIdProviderDiscoveryService;
import it.smartcommunitylab.aac.openidfed.service.StaticOpenIdProviderDiscoveryService;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

public class OpenIdFedIdentityProviderConfig
    extends AbstractIdentityProviderConfig<OpenIdFedIdentityProviderConfigMap> {

    private static final long serialVersionUID = SystemKeys.AAC_OPENIDFED_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR + OpenIdFedIdentityProviderConfigMap.RESOURCE_TYPE;

    @JsonIgnore
    private transient OpenIdProviderDiscoveryService providerService;

    @JsonIgnore
    private transient OpenIdFedClientRegistrationRepository clientRegistrationRepository;

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

    public OpenIdProviderDiscoveryService getProviderService() {
        if (providerService == null) {
            //build new service based on config
            if (configMap.getProviders() != null && !configMap.getProviders().isEmpty()) {
                Set<String> providers = configMap
                    .getProviders()
                    .stream()
                    .map(p -> p.getValue())
                    .collect(Collectors.toSet());
                providerService = new StaticOpenIdProviderDiscoveryService(configMap.getTrustAnchor(), providers);
            } else {
                providerService = new ListingOpenIdProviderDiscoveryService(configMap.getTrustAnchor());
            }
        }
        return providerService;
    }

    public OpenIdFedClientRegistrationRepository getClientRegistrationRepository() {
        if (clientRegistrationRepository == null) {
            clientRegistrationRepository =
                new OpenIdFedClientRegistrationRepository(getConfigMap(), getProviderService(), getRedirectUrl());
        }

        return clientRegistrationRepository;
    }

    public String getClientId() {
        //if set use configMap value - note: should match urls
        if (StringUtils.hasText(configMap.getClientId())) {
            return configMap.getClientId();
        }

        //build url as base to be inflated
        return "{baseUrl}/auth/" + getAuthority() + "/id/" + getProvider();
    }

    public String getRepositoryId() {
        // not configurable, always isolate oidc providers
        return getProvider();
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

    public Set<String> getScopes() {
        String[] scope = StringUtils.commaDelimitedListToStringArray(configMap.getScope());

        //always add "openid"
        Set<String> scopes = new HashSet<>(Arrays.asList(scope));
        scopes.add("openid");

        return scopes;
    }

    public String getRedirectUrl() {
        return "{baseUrl}/auth/" + getAuthority() + "/{action}/" + getProvider();
    }
}
