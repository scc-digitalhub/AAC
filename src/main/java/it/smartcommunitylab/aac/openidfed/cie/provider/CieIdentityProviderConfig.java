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

package it.smartcommunitylab.aac.openidfed.cie.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.openid.connect.sdk.SubjectType;
import com.nimbusds.openid.connect.sdk.federation.registration.ClientRegistrationType;
import com.nimbusds.openid.connect.sdk.rp.ApplicationType;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityProviderConfig;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.identity.provider.IdentityProviderSettingsMap;
import it.smartcommunitylab.aac.openidfed.cie.auth.CieClientRegistrationRepository;
import it.smartcommunitylab.aac.openidfed.provider.OpenIdFedIdentityProviderConfig;
import it.smartcommunitylab.aac.openidfed.provider.OpenIdFedIdentityProviderConfigMap;
import it.smartcommunitylab.aac.openidfed.resolvers.CachingEntityStatementResolver;
import it.smartcommunitylab.aac.openidfed.resolvers.EntityStatementResolver;
import it.smartcommunitylab.aac.openidfed.service.ListingOpenIdProviderDiscoveryService;
import it.smartcommunitylab.aac.openidfed.service.OpenIdProviderDiscoveryService;
import it.smartcommunitylab.aac.openidfed.service.StaticOpenIdProviderDiscoveryService;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

public class CieIdentityProviderConfig extends AbstractIdentityProviderConfig<CieIdentityProviderConfigMap> {

    private static final long serialVersionUID = SystemKeys.AAC_CIE_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR + CieIdentityProviderConfigMap.RESOURCE_TYPE;

    @JsonIgnore
    private transient EntityStatementResolver entityStatementResolver;

    @JsonIgnore
    private transient OpenIdProviderDiscoveryService providerService;

    @JsonIgnore
    private transient CieClientRegistrationRepository clientRegistrationRepository;

    public CieIdentityProviderConfig(String provider, String realm) {
        this(SystemKeys.AUTHORITY_CIE, provider, realm);
    }

    public CieIdentityProviderConfig(String authority, String provider, String realm) {
        super(authority, provider, realm, new IdentityProviderSettingsMap(), new CieIdentityProviderConfigMap());
    }

    public CieIdentityProviderConfig(
        ConfigurableIdentityProvider cp,
        IdentityProviderSettingsMap settingsMap,
        CieIdentityProviderConfigMap configMap
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
    private CieIdentityProviderConfig() {
        super();
    }

    public EntityStatementResolver getEntityStatementResolver() {
        if (entityStatementResolver == null) {
            //use a caching resolver to keep track of resolved entities
            this.entityStatementResolver = new CachingEntityStatementResolver();
        }

        return entityStatementResolver;
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
                providerService =
                    new StaticOpenIdProviderDiscoveryService(
                        configMap.getTrustAnchor(),
                        providers,
                        getEntityStatementResolver()
                    );
            } else {
                providerService =
                    new ListingOpenIdProviderDiscoveryService(configMap.getTrustAnchor(), getEntityStatementResolver());
            }
        }
        return providerService;
    }

    public CieClientRegistrationRepository getClientRegistrationRepository() {
        if (clientRegistrationRepository == null) {
            clientRegistrationRepository =
                new CieClientRegistrationRepository(getConfigMap(), getProviderService(), getRedirectUrl());
        }

        return clientRegistrationRepository;
    }

    public String getClientId() {
        //if set use configMap value - note: should match urls
        if (StringUtils.hasText(configMap.getClientId())) {
            return configMap.getClientId();
        }

        //build url as base to be inflated
        return "{baseUrl}/auth/" + getAuthority() + "/metadata/" + getProvider();
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

    public JWKSet getClientJWKSet() {
        if (!StringUtils.hasText(configMap.getClientJwks())) {
            return null;
        }

        // expect a single key as jwk
        try {
            return JWKSet.parse(configMap.getClientJwks());
        } catch (ParseException e) {
            return null;
        }
    }

    public JWK getClientSignatureJWK() {
        return getClientJWKSet() == null
            ? null
            : getClientJWKSet()
                .getKeys()
                .stream()
                .filter(k -> k.getKeyUse().equals(KeyUse.SIGNATURE))
                .findFirst()
                .orElse(null);
    }

    public JWK getClientEncryptionJWK() {
        return getClientJWKSet() == null
            ? null
            : getClientJWKSet()
                .getKeys()
                .stream()
                .filter(k -> k.getKeyUse().equals(KeyUse.ENCRYPTION))
                .findFirst()
                .orElse(null);
    }

    public JWK getFederationJWK() {
        if (!StringUtils.hasText(configMap.getFederationJwks())) {
            //fall back to client keys
            return getClientSignatureJWK();
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

    public Set<String> getSupportedLoaFromAcrValues(String acrValues, Boolean decode) {
        String decodedAcrValues = acrValues;
        if (decode) {
            decodedAcrValues = clientRegistrationRepository.decode(acrValues);
        }
        String[] loa = StringUtils.commaDelimitedListToStringArray(decodedAcrValues);

        return new HashSet<>(Arrays.asList(loa))
            .stream()
            .filter(l -> configMap.getSupportedAcrValuesForLoa().containsKey(l))
            .map(l -> configMap.getSupportedAcrValuesForLoa().get(l))
            .collect(Collectors.toSet());
    }

    public Set<String> getSupportedClaimsFromAcrValues(String acrValues, Boolean decode) {
        String decodedAcrValues = acrValues;
        if (decode) {
            decodedAcrValues = clientRegistrationRepository.decode(acrValues);
        }
        String[] claims = StringUtils.commaDelimitedListToStringArray(decodedAcrValues);

        return new HashSet<>(Arrays.asList(claims))
            .stream()
            .filter(a -> configMap.getSupportedAcrValuesForClaims().containsKey(a))
            .flatMap(a ->
                Arrays.stream(
                    StringUtils.commaDelimitedListToStringArray(configMap.getSupportedAcrValuesForClaims().get(a))
                )
            )
            .collect(Collectors.toSet());
    }

    public String getRedirectUrl() {
        return "{baseUrl}/auth/" + getAuthority() + "/{action}/" + getProvider();
    }

    @JsonIgnore
    public OpenIdFedIdentityProviderConfig toOpenIdFedIdentityProviderConfig() {
        OpenIdFedIdentityProviderConfig op = new OpenIdFedIdentityProviderConfig(
            SystemKeys.AUTHORITY_CIE,
            getProvider(),
            getRealm()
        );
        OpenIdFedIdentityProviderConfigMap cMap = new OpenIdFedIdentityProviderConfigMap();
        cMap.setClientId(configMap.getClientId());
        cMap.setClientJwks(configMap.getClientJwks());
        cMap.setClientName(configMap.getClientName());
        cMap.setScope(configMap.getScope());
        cMap.setUserNameAttributeName(configMap.getUserNameAttributeName());
        cMap.setTrustEmailAddress(configMap.getTrustEmailAddress());
        cMap.setRequireEmailAddress(configMap.getRequireEmailAddress());
        cMap.setAlwaysTrustEmailAddress(configMap.getAlwaysTrustEmailAddress());
        cMap.setRespectTokenExpiration(configMap.getRespectTokenExpiration());
        cMap.setPromptMode(configMap.getPromptMode());
        cMap.setTrustAnchor(configMap.getTrustAnchor());
        cMap.setProviders(configMap.getProviders());
        cMap.setFederationJwks(configMap.getFederationJwks());
        cMap.setAuthorityHints(configMap.getAuthorityHints());
        cMap.setTrustMarks(configMap.getTrustMarks());
        cMap.setSubjectType(configMap.getSubjectType());
        cMap.setClaims(new HashSet<>(Arrays.asList(configMap.getDefaultAcrValueForClaims())));
        cMap.setAcrValues(new HashSet<>(Arrays.asList(configMap.getDefaultAcrValueForLoa())));
        cMap.setUserInfoJWEAlg(configMap.getUserInfoJWEAlg());
        cMap.setUserInfoJWEEnc(configMap.getUserInfoJWEEnc());
        cMap.setOrganizationName(configMap.getOrganizationName());
        cMap.setContacts(configMap.getContacts());
        op.setConfigMap(cMap);

        return op;
    }
}
