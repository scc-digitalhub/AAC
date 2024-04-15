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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.ConfigurableProperties;
import it.smartcommunitylab.aac.oauth.model.ApplicationType;
import it.smartcommunitylab.aac.oauth.model.AuthenticationMethod;
import it.smartcommunitylab.aac.oauth.model.AuthorizationGrantType;
import it.smartcommunitylab.aac.oauth.model.SubjectType;
import it.smartcommunitylab.aac.oauth.model.TokenType;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.validation.Valid;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class OAuth2ClientConfigMap implements ConfigurableProperties, Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_OAUTH2_SERIAL_VERSION;

    private static ObjectMapper mapper = new ObjectMapper();
    private static final TypeReference<HashMap<String, Serializable>> typeRef =
        new TypeReference<HashMap<String, Serializable>>() {};

    private Set<AuthorizationGrantType> authorizedGrantTypes;
    private Set<String> redirectUris;

    private ApplicationType applicationType;
    private TokenType tokenType;
    private Set<AuthenticationMethod> authenticationMethods;

    private SubjectType subjectType;

    private Boolean idTokenClaims;
    private Boolean firstParty;

    private Integer idTokenValidity;
    private Integer accessTokenValidity;
    private Integer refreshTokenValidity;

    //    private String jwks;
    private String jwksUri;

    // additional configuration
    @JsonUnwrapped
    private OAuth2ClientAdditionalConfig additionalConfig;

    @JsonUnwrapped
    private OAuth2ClientInfo additionalInformation;

    public OAuth2ClientConfigMap() {
        authorizedGrantTypes = new HashSet<>();
        redirectUris = new HashSet<>();
        authenticationMethods = new HashSet<>();
        tokenType = TokenType.JWT;
        applicationType = ApplicationType.WEB;
        subjectType = SubjectType.PUBLIC;
    }

    public OAuth2ClientConfigMap(Map<String, Serializable> props) {
        authorizedGrantTypes = new HashSet<>();
        redirectUris = new HashSet<>();
        authenticationMethods = new HashSet<>();
        tokenType = TokenType.JWT;
        applicationType = ApplicationType.WEB;
        subjectType = SubjectType.PUBLIC;

        setConfiguration(props);
    }

    public Set<AuthorizationGrantType> getAuthorizedGrantTypes() {
        return authorizedGrantTypes;
    }

    public void setAuthorizedGrantTypes(Set<AuthorizationGrantType> authorizedGrantTypes) {
        this.authorizedGrantTypes = authorizedGrantTypes;
    }

    public Set<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(Set<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public ApplicationType getApplicationType() {
        return applicationType;
    }

    public void setApplicationType(ApplicationType applicationType) {
        this.applicationType = applicationType;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType(TokenType tokenType) {
        this.tokenType = tokenType;
    }

    public SubjectType getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(SubjectType subjectType) {
        this.subjectType = subjectType;
    }

    public Set<AuthenticationMethod> getAuthenticationMethods() {
        return authenticationMethods;
    }

    public void setAuthenticationMethods(Set<AuthenticationMethod> authenticationMethods) {
        this.authenticationMethods = authenticationMethods;
    }

    public Boolean getIdTokenClaims() {
        return idTokenClaims;
    }

    public void setIdTokenClaims(Boolean idTokenClaims) {
        this.idTokenClaims = idTokenClaims;
    }

    public Boolean getFirstParty() {
        return firstParty;
    }

    public void setFirstParty(Boolean firstParty) {
        this.firstParty = firstParty;
    }

    public boolean isRefreshTokenRotation() {
        if (additionalConfig == null) {
            return false;
        }

        return additionalConfig.getRefreshTokenRotation() != null
            ? additionalConfig.getRefreshTokenRotation().booleanValue()
            : false;
    }

    public Integer getAccessTokenValidity() {
        return accessTokenValidity;
    }

    public void setAccessTokenValidity(Integer accessTokenValidity) {
        this.accessTokenValidity = accessTokenValidity;
    }

    public Integer getRefreshTokenValidity() {
        return refreshTokenValidity;
    }

    public void setRefreshTokenValidity(Integer refreshTokenValidity) {
        this.refreshTokenValidity = refreshTokenValidity;
    }

    public Integer getIdTokenValidity() {
        return idTokenValidity;
    }

    public void setIdTokenValidity(Integer idTokenValidity) {
        this.idTokenValidity = idTokenValidity;
    }

    //    public String getJwks() {
    //        return jwks;
    //    }
    //
    //    public void setJwks(String jwks) {
    //        this.jwks = jwks;
    //    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    @JsonIgnore
    public OAuth2ClientAdditionalConfig getAdditionalConfig() {
        return additionalConfig;
    }

    public void setAdditionalConfig(OAuth2ClientAdditionalConfig additionalConfig) {
        this.additionalConfig = additionalConfig;
    }

    @JsonIgnore
    public OAuth2ClientInfo getAdditionalInformation() {
        return additionalInformation;
    }

    public void setAdditionalInformation(OAuth2ClientInfo additionalInformation) {
        this.additionalInformation = additionalInformation;
    }

    //    @JsonIgnore
    //    public JWKSet getJwksSet() {
    //        // read from string or load from uri
    //        // TODO
    //        if (!StringUtils.hasText(jwks)) {
    //            return null;
    //        }
    //
    //        try {
    //            return JWKSet.parse(jwks);
    //        } catch (ParseException e) {
    //            return null;
    //        }
    //    }

    @Override
    @JsonIgnore
    public Map<String, Serializable> getConfiguration() {
        // use mapper
        mapper.setSerializationInclusion(Include.ALWAYS);
        return mapper.convertValue(this, typeRef);
    }

    @Override
    @JsonIgnore
    public void setConfiguration(Map<String, Serializable> props) {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        OAuth2ClientConfigMap map = mapper.convertValue(props, OAuth2ClientConfigMap.class);
        this.authorizedGrantTypes = map.getAuthorizedGrantTypes();
        this.redirectUris = map.getRedirectUris();

        this.applicationType = map.getApplicationType();
        this.tokenType = map.getTokenType();
        this.subjectType = map.getSubjectType();

        this.authenticationMethods = map.getAuthenticationMethods();

        this.idTokenClaims = map.getIdTokenClaims();
        this.firstParty = map.getFirstParty();

        this.idTokenValidity = map.getIdTokenValidity();
        this.accessTokenValidity = map.getAccessTokenValidity();
        this.refreshTokenValidity = map.getRefreshTokenValidity();

        //        this.jwks = map.getJwks();
        this.jwksUri = map.getJwksUri();

        // handle additional props
        // we parse again the map because we flatten props by unwrapping
        OAuth2ClientAdditionalConfig additionalConfig = OAuth2ClientAdditionalConfig.convert(props);
        if (additionalConfig != null) {
            this.additionalConfig = additionalConfig;
        }
        OAuth2ClientInfo clientInfo = OAuth2ClientInfo.convert(props);
        if (clientInfo != null) {
            this.additionalInformation = clientInfo;
        }
        //        if(props != null && props.containsKey("additionalInformation")) {
        //            this.additionalInformation = OAuth2ClientInfo.convert(props.get("additionalInformation"));
        //        }

    }

    @JsonIgnore
    public static JsonSchema getConfigurationSchema() throws JsonMappingException {
        JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(mapper);
        return schemaGen.generateSchema(OAuth2ClientConfigMap.class);
    }
}
