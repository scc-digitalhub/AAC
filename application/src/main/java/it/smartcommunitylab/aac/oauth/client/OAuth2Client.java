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

package it.smartcommunitylab.aac.oauth.client;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.clients.base.BaseClient;
import it.smartcommunitylab.aac.clients.persistence.ClientEntity;
import it.smartcommunitylab.aac.core.model.ConfigurableProperties;
import it.smartcommunitylab.aac.oauth.model.ApplicationType;
import it.smartcommunitylab.aac.oauth.model.AuthenticationMethod;
import it.smartcommunitylab.aac.oauth.model.AuthorizationGrantType;
import it.smartcommunitylab.aac.oauth.model.ClientJwks;
import it.smartcommunitylab.aac.oauth.model.ClientSecret;
import it.smartcommunitylab.aac.oauth.model.SubjectType;
import it.smartcommunitylab.aac.oauth.model.TokenType;
import it.smartcommunitylab.aac.oauth.persistence.OAuth2ClientEntity;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

public class OAuth2Client extends BaseClient implements ConfigurableProperties {

    private static final long serialVersionUID = SystemKeys.AAC_OAUTH2_SERIAL_VERSION;

    public static final String CLIENT_TYPE = SystemKeys.CLIENT_TYPE_OAUTH2;

    @JsonIgnore
    private ClientSecret clientSecret;

    @JsonIgnore
    private ClientJwks jwks;

    //    private Set<AuthorizationGrantType> authorizedGrantTypes;
    //    private Set<String> redirectUris;
    //
    //    private TokenType tokenType;
    //    private Set<AuthenticationMethod> authenticationMethods;
    //
    //    private Boolean firstParty;
    //    private Set<String> autoApproveScopes;
    //
    //    private Integer accessTokenValidity;
    //    private Integer refreshTokenValidity;
    //
    //    private JWSAlgorithm jwtSignAlgorithm;
    //    private JWEAlgorithm jwtEncAlgorithm;
    //    private EncryptionMethod jwtEncMethod;
    //    private JWKSet jwks;
    //    private String jwksUri;

    @JsonUnwrapped
    private OAuth2ClientConfigMap configMap;

    public OAuth2Client(@JsonProperty("realm") String realm, @JsonProperty("clientId") String clientId) {
        super(realm, clientId);
        configMap = new OAuth2ClientConfigMap();
    }

    @Override
    @JsonIgnore
    public String getType() {
        return SystemKeys.CLIENT_TYPE_OAUTH2;
    }

    @JsonProperty("clientSecret")
    public String getSecret() {
        return (clientSecret != null ? clientSecret.getCredentials() : null);
    }

    @JsonProperty("clientSecret")
    public void setSecret(String secret) {
        this.clientSecret = new ClientSecret(getRealm(), getClientId(), secret);
    }

    public ClientSecret getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(ClientSecret clientSecret) {
        if (clientSecret == null) {
            this.clientSecret = null;
        } else {
            this.clientSecret = new ClientSecret(getRealm(), getClientId(), clientSecret.getClientSecret());
        }
    }

    @JsonProperty("jwks")
    public String getJwks() {
        return (jwks != null ? jwks.getCredentials() : null);
    }

    @JsonProperty("clientSecret")
    public void setJwks(String jwks) {
        this.jwks = new ClientJwks(getRealm(), getClientId(), jwks);
    }

    public ClientJwks getClientJwks() {
        return jwks;
    }

    public void setClientJwks(ClientJwks jwks) {
        if (jwks == null) {
            this.jwks = null;
        } else {
            this.jwks = new ClientJwks(getRealm(), getClientId(), jwks.getJwks());
        }
    }

    //    public Set<AuthorizationGrantType> getAuthorizedGrantTypes() {
    //        return authorizedGrantTypes;
    //    }
    //
    //    public void setAuthorizedGrantTypes(Set<AuthorizationGrantType> authorizedGrantTypes) {
    //        this.authorizedGrantTypes = authorizedGrantTypes;
    //    }
    //
    //    public TokenType getTokenType() {
    //        return tokenType;
    //    }
    //
    //    public void setTokenType(TokenType tokenType) {
    //        this.tokenType = tokenType;
    //    }
    //
    //    public Set<AuthenticationMethod> getAuthenticationMethods() {
    //        return authenticationMethods;
    //    }
    //
    //    public void setAuthenticationMethods(Set<AuthenticationMethod> authenticationMethods) {
    //        this.authenticationMethods = authenticationMethods;
    //    }
    //
    //    public Set<String> getRedirectUris() {
    //        return redirectUris;
    //    }
    //
    //    public void setRedirectUris(Set<String> redirectUris) {
    //        this.redirectUris = redirectUris;
    //    }
    //
    //    public Boolean getFirstParty() {
    //        return firstParty;
    //    }
    //
    //    public void setFirstParty(Boolean firstParty) {
    //        this.firstParty = firstParty;
    //    }
    //
    //    public Set<String> getAutoApproveScopes() {
    //        return autoApproveScopes;
    //    }
    //
    //    public void setAutoApproveScopes(Set<String> autoApproveScopes) {
    //        this.autoApproveScopes = autoApproveScopes;
    //    }
    //
    //    public Integer getAccessTokenValidity() {
    //        return accessTokenValidity;
    //    }
    //
    //    public void setAccessTokenValidity(Integer accessTokenValidity) {
    //        this.accessTokenValidity = accessTokenValidity;
    //    }
    //
    //    public Integer getRefreshTokenValidity() {
    //        return refreshTokenValidity;
    //    }
    //
    //    public void setRefreshTokenValidity(Integer refreshTokenValidity) {
    //        this.refreshTokenValidity = refreshTokenValidity;
    //    }
    //
    //    public JWSAlgorithm getJwtSignAlgorithm() {
    //        return jwtSignAlgorithm;
    //    }
    //
    //    public void setJwtSignAlgorithm(JWSAlgorithm jwtSignAlgorithm) {
    //        this.jwtSignAlgorithm = jwtSignAlgorithm;
    //    }
    //
    //    public JWEAlgorithm getJwtEncAlgorithm() {
    //        return jwtEncAlgorithm;
    //    }
    //
    //    public void setJwtEncAlgorithm(JWEAlgorithm jwtEncAlgorithm) {
    //        this.jwtEncAlgorithm = jwtEncAlgorithm;
    //    }
    //
    //    public EncryptionMethod getJwtEncMethod() {
    //        return jwtEncMethod;
    //    }
    //
    //    public void setJwtEncMethod(EncryptionMethod jwtEncMethod) {
    //        this.jwtEncMethod = jwtEncMethod;
    //    }
    //
    //    public JWKSet getJwks() {
    //        return jwks;
    //    }
    //
    //    public void setJwks(JWKSet jwks) {
    //        this.jwks = jwks;
    //    }
    //
    //    public String getJwksUri() {
    //        return jwksUri;
    //    }
    //
    //    public void setJwksUri(String jwksUri) {
    //        this.jwksUri = jwksUri;
    //    }

    //    @JsonIgnore
    //    public OAuth2ClientInfo getAdditionalInformation() {
    //        return additionalInformation;
    //    }
    //
    //    public void setAdditionalInformation(OAuth2ClientInfo additionalInformation) {
    //        this.additionalInformation = additionalInformation;
    //    }

    @JsonIgnore
    public OAuth2ClientConfigMap getConfigMap() {
        return configMap;
    }

    public void setConfigMap(OAuth2ClientConfigMap configMap) {
        this.configMap = configMap;
    }

    @Override
    public Map<String, Serializable> getConfiguration() {
        Map<String, Serializable> map = configMap.getConfiguration();
        // add credentials
        map.put("clientSecret", getSecret());
        map.put("jwks", getJwks());
        return map;
    }

    @Override
    public void setConfiguration(Map<String, Serializable> props) {
        configMap = new OAuth2ClientConfigMap();
        configMap.setConfiguration(props);
    }

    //    @Override
    //    @JsonIgnore
    //    public Map<String, Serializable> getConfigurationMap() {
    //        return getJacksonConfigurationMap();
    //    }
    //
    //    @JsonIgnore
    //    public Map<String, Serializable> getJacksonConfigurationMap() {
    //        mapper.setSerializationInclusion(Include.NON_EMPTY);
    //        return mapper.convertValue(this, typeRef);
    //    }
    //
    //    @JsonIgnore
    //    public Map<String, Serializable> getStaticConfigurationMap() {
    //        // TODO fix naming to follow RFC standard and return TLD elements
    //        // will break converter
    //        try {
    //            Map<String, Serializable> map = new HashMap<>();
    //            // expose only specific properties
    //
    //            if (clientSecret != null) {
    //                map.put("clientSecret", clientSecret.getCredentials());
    //            }
    //
    //            // TODO rewrite properly
    //            if (authorizedGrantTypes != null) {
    //                map.put("authorizedGrantTypes", authorizedGrantTypes.toArray(new AuthorizationGrantType[0]));
    //            } else {
    //                map.put("authorizedGrantTypes", new AuthorizationGrantType[0]);
    //            }
    //
    //            if (redirectUris != null) {
    //                map.put("redirectUris", redirectUris.toArray(new String[0]));
    //            } else {
    //                map.put("redirectUris", new String[0]);
    //            }
    //
    //            if (authenticationMethods != null) {
    //                map.put("authenticationMethods", authenticationMethods.toArray(new AuthenticationMethod[0]));
    //            } else {
    //                map.put("authenticationMethods", new AuthenticationMethod[0]);
    //            }
    //
    //            map.put("tokenType", tokenType);
    //
    //            map.put("firstParty", firstParty);
    //            if (autoApproveScopes != null) {
    //                map.put("autoApproveScopes", autoApproveScopes.toArray(new String[0]));
    //            } else {
    //                map.put("autoApproveScopes", new String[0]);
    //            }
    //
    //            map.put("accessTokenValidity", accessTokenValidity);
    //            map.put("refreshTokenValidity", refreshTokenValidity);
    //
    //            if (jwtSignAlgorithm != null) {
    //                map.put("jwtSignAlgorithm", jwtSignAlgorithm.getValue());
    //            }
    //
    //            if (jwtEncMethod != null) {
    //                map.put("jwtEncMethod", jwtEncMethod.getValue());
    //            }
    //
    //            if (jwtEncAlgorithm != null) {
    //                map.put("jwtEncAlgorithm", jwtEncAlgorithm.getValue());
    //            }
    //
    //            map.put("jwks", jwks);
    //            map.put("jwksUri", jwksUri);
    //
    //            // filter null
    //            return map.entrySet().stream().filter(e -> e.getValue() != null)
    //                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    //
    //        } catch (Exception e) {
    //            return null;
    //        }
    //
    //    }

    //    private String toJson() throws JsonProcessingException {
    //        mapper.setSerializationInclusion(Include.NON_EMPTY);
    //        return mapper.writeValueAsString(this);
    //    }

    /*
     * builder
     *
     * TODO add a proper builder to use with jackson
     */

    //    public static OAuth2Client convert(Map<String, Serializable> map) {
    //        // use mapper
    //        // TODO custom mapping, see getConfigurationMap
    //        return mapper.convertValue(map, OAuth2Client.class);
    //    }

    public static OAuth2Client from(ClientEntity client, OAuth2ClientEntity oauth) {
        OAuth2Client c = new OAuth2Client(client.getRealm(), client.getClientId());
        c.setName(client.getName());
        c.setDescription(client.getDescription());

        c.setProviders(StringUtils.commaDelimitedListToSet(client.getProviders()));

        c.setScopes(StringUtils.commaDelimitedListToSet(client.getScopes()));
        c.setResourceIds(StringUtils.commaDelimitedListToSet(client.getResourceIds()));

        c.setHookFunctions(client.getHookFunctions());
        c.setHookWebUrls(client.getHookWebUrls());
        c.setHookUniqueSpaces(client.getHookUniqueSpaces());

        // map attributes
        // oauth2 config map
        c.configMap = new OAuth2ClientConfigMap();

        c.configMap.setApplicationType(
            (
                oauth.getApplicationType() != null
                    ? ApplicationType.parse(oauth.getApplicationType())
                    : ApplicationType.WEB
            )
        );
        c.configMap.setTokenType(
            (oauth.getTokenType() != null ? TokenType.parse(oauth.getTokenType()) : TokenType.JWT)
        );
        c.configMap.setSubjectType(
            (oauth.getSubjectType() != null ? SubjectType.parse(oauth.getSubjectType()) : SubjectType.PUBLIC)
        );

        Set<String> authorizedGrantTypes = StringUtils.commaDelimitedListToSet(oauth.getAuthorizedGrantTypes());
        c.configMap.setAuthorizedGrantTypes(
            authorizedGrantTypes
                .stream()
                .map(a -> AuthorizationGrantType.parse(a))
                .filter(a -> (a != null))
                .collect(Collectors.toSet())
        );

        Set<String> authenticationMethods = StringUtils.commaDelimitedListToSet(oauth.getAuthenticationMethods());
        c.configMap.setAuthenticationMethods(
            authenticationMethods
                .stream()
                .map(a -> AuthenticationMethod.parse(a))
                .filter(a -> (a != null))
                .collect(Collectors.toSet())
        );

        c.configMap.setRedirectUris(StringUtils.commaDelimitedListToSet(oauth.getRedirectUris()));
        c.configMap.setIdTokenClaims(oauth.isIdTokenClaims());
        c.configMap.setFirstParty(oauth.isFirstParty());

        c.configMap.setIdTokenValidity(oauth.getIdTokenValidity());
        c.configMap.setAccessTokenValidity(oauth.getAccessTokenValidity());
        c.configMap.setRefreshTokenValidity(oauth.getRefreshTokenValidity());

        //        if (oauth.getConfigurationMap() != null) {
        //            Map<String, Serializable> configMap = oauth.getConfigurationMap();
        //
        //            String jwtSignAlgorithm = (String) configMap.get("jwt_sign_alg");
        //            String jwtEncAlgorithm = (String) configMap.get("jwt_enc_alg");
        //            String jwtEncMethod = (String) configMap.get("jwt_enc_method");
        //
        //            JWTConfig jwtConfig = new JWTConfig();
        //
        //            jwtConfig.setSignAlgorithm(jwtSignAlgorithm != null ? JWSAlgorithm.parse(jwtSignAlgorithm)
        //                    : null);
        //            jwtConfig.setEncAlgorithm(jwtEncAlgorithm != null ? JWEAlgorithm.parse(jwtEncAlgorithm)
        //                    : null);
        //            jwtConfig.setEncMethod(jwtEncMethod != null ? EncryptionMethod.parse(jwtEncMethod) : null);
        //        }

        //        c.configMap.setJwks(oauth.getJwks());
        c.configMap.setJwksUri(oauth.getJwksUri());

        Map<String, Serializable> additionalConfig = oauth.getAdditionalConfiguration();
        if (additionalConfig != null) {
            c.configMap.setAdditionalConfig(OAuth2ClientAdditionalConfig.convert(additionalConfig));
        }

        //        c.tokenType = (oauth.getTokenType() != null ? TokenType.parse(oauth.getTokenType()) : null);
        //
        //        Set<String> authorizedGrantTypes = StringUtils.commaDelimitedListToSet(oauth.getAuthorizedGrantTypes());
        //        c.authorizedGrantTypes = authorizedGrantTypes.stream()
        //                .map(a -> AuthorizationGrantType.parse(a))
        //                .filter(a -> (a != null))
        //                .collect(Collectors.toSet());
        //
        //        Set<String> authenticationMethods = StringUtils.commaDelimitedListToSet(oauth.getAuthenticationMethods());
        //        c.authenticationMethods = authenticationMethods.stream()
        //                .map(a -> AuthenticationMethod.parse(a))
        //                .filter(a -> (a != null))
        //                .collect(Collectors.toSet());
        //
        //        c.redirectUris.addAll(StringUtils.commaDelimitedListToSet(oauth.getRedirectUris()));
        //        c.setResourceIds(StringUtils.commaDelimitedListToSet(oauth.getResourceIds()));
        //
        //        c.setFirstParty(oauth.isFirstParty());
        //        c.setAutoApproveScopes(StringUtils.commaDelimitedListToSet(oauth.getAutoApproveScopes()));
        //
        //        c.accessTokenValidity = oauth.getAccessTokenValidity();
        //        c.refreshTokenValidity = oauth.getRefreshTokenValidity();
        //
        //        c.jwtSignAlgorithm = (oauth.getJwtSignAlgorithm() != null ? JWSAlgorithm.parse(oauth.getJwtSignAlgorithm())
        //                : null);
        //        c.jwtEncAlgorithm = (oauth.getJwtEncAlgorithm() != null ? JWEAlgorithm.parse(oauth.getJwtEncAlgorithm())
        //                : null);
        //        c.jwtEncMethod = (oauth.getJwtEncMethod() != null ? EncryptionMethod.parse(oauth.getJwtEncMethod()) : null);
        //        try {
        //            c.jwks = (StringUtils.hasText(oauth.getJwks()) ? JWKSet.parse(oauth.getJwks()) : null);
        //        } catch (ParseException e) {
        //            c.jwks = null;
        //        }
        //        c.jwksUri = oauth.getJwksUri();

        Map<String, Serializable> additionalInfo = oauth.getAdditionalInformation();
        if (additionalInfo != null) {
            c.configMap.setAdditionalInformation(OAuth2ClientInfo.convert(additionalInfo));
            //            c.additionalInformation = OAuth2ClientInfo.convert(map);
        }

        // credentials
        if (oauth.getClientSecret() != null) {
            c.setSecret(oauth.getClientSecret());
        }
        if (oauth.getJwks() != null) {
            c.setJwks(oauth.getJwks());
        }

        return c;
    }
}
