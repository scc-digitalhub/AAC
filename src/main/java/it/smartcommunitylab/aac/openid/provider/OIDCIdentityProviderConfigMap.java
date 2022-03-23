package it.smartcommunitylab.aac.openid.provider;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.ConfigurableProperties;
import it.smartcommunitylab.aac.oauth.model.AuthenticationMethod;
import it.smartcommunitylab.aac.oauth.model.PromptMode;
import it.smartcommunitylab.aac.openid.model.OIDCUserAttribute;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class OIDCIdentityProviderConfigMap implements ConfigurableProperties, Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_OIDC_SERIAL_VERSION;

    private static ObjectMapper mapper = new ObjectMapper();
    private final static TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<HashMap<String, Serializable>>() {
    };

    @NotBlank
    private String clientId;
    private String clientSecret;
    private String clientName;

    private AuthenticationMethod clientAuthenticationMethod;
    private String scope;
    private String userNameAttributeName;
    private Boolean trustEmailAddress;
    private Boolean requireEmailAddress;
    private Boolean alwaysTrustEmailAddress;

    private OIDCUserAttribute idAttribute;

    // explicit config
    private String authorizationUri;
    private String tokenUri;
    private String jwkSetUri;
    private String userInfoUri;

    // autoconfiguration support from well-known
    private String issuerUri;

    // session control
    private Boolean propagateEndSession;
    private Boolean respectTokenExpiration;
    private Set<PromptMode> promptMode;

    public OIDCIdentityProviderConfigMap() {
        // set default
        this.scope = "openid,email";
        this.userNameAttributeName = IdTokenClaimNames.SUB;
        this.clientAuthenticationMethod = AuthenticationMethod.CLIENT_SECRET_BASIC;
        this.idAttribute = OIDCUserAttribute.EMAIL;
        this.trustEmailAddress = true;
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

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public AuthenticationMethod getClientAuthenticationMethod() {
        return clientAuthenticationMethod;
    }

    public void setClientAuthenticationMethod(AuthenticationMethod clientAuthenticationMethod) {
        this.clientAuthenticationMethod = clientAuthenticationMethod;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getUserNameAttributeName() {
        return userNameAttributeName;
    }

    public void setUserNameAttributeName(String userNameAttributeName) {
        this.userNameAttributeName = userNameAttributeName;
    }

    public Boolean getTrustEmailAddress() {
        return trustEmailAddress;
    }

    public void setTrustEmailAddress(Boolean trustEmailAddress) {
        this.trustEmailAddress = trustEmailAddress;
    }

    public Boolean getAlwaysTrustEmailAddress() {
        return alwaysTrustEmailAddress;
    }

    public void setAlwaysTrustEmailAddress(Boolean alwaysTrustEmailAddress) {
        this.alwaysTrustEmailAddress = alwaysTrustEmailAddress;
    }

    public Boolean getRequireEmailAddress() {
        return requireEmailAddress;
    }

    public void setRequireEmailAddress(Boolean requireEmailAddress) {
        this.requireEmailAddress = requireEmailAddress;
    }

    public String getAuthorizationUri() {
        return authorizationUri;
    }

    public void setAuthorizationUri(String authorizationUri) {
        this.authorizationUri = authorizationUri;
    }

    public String getTokenUri() {
        return tokenUri;
    }

    public void setTokenUri(String tokenUri) {
        this.tokenUri = tokenUri;
    }

    public String getJwkSetUri() {
        return jwkSetUri;
    }

    public void setJwkSetUri(String jwkSetUri) {
        this.jwkSetUri = jwkSetUri;
    }

    public String getUserInfoUri() {
        return userInfoUri;
    }

    public void setUserInfoUri(String userInfoUri) {
        this.userInfoUri = userInfoUri;
    }

    public String getIssuerUri() {
        return issuerUri;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }

    public Boolean getPropagateEndSession() {
        return propagateEndSession;
    }

    public void setPropagateEndSession(Boolean propagateEndSession) {
        this.propagateEndSession = propagateEndSession;
    }

    public Boolean getRespectTokenExpiration() {
        return respectTokenExpiration;
    }

    public void setRespectTokenExpiration(Boolean respectTokenExpiration) {
        this.respectTokenExpiration = respectTokenExpiration;
    }

    public Set<PromptMode> getPromptMode() {
        return promptMode;
    }

    public void setPromptMode(Set<PromptMode> promptMode) {
        this.promptMode = promptMode;
    }

    public OIDCUserAttribute getIdAttribute() {
        return idAttribute;
    }

    public void setIdAttribute(OIDCUserAttribute idAttribute) {
        this.idAttribute = idAttribute;
    }

    @Override
    @JsonIgnore
    public Map<String, Serializable> getConfiguration() {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        return mapper.convertValue(this, typeRef);
    }

    @Override
    @JsonIgnore
    public void setConfiguration(Map<String, Serializable> props) {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        OIDCIdentityProviderConfigMap map = mapper.convertValue(props, OIDCIdentityProviderConfigMap.class);

        this.clientId = map.getClientId();
        this.clientSecret = map.getClientSecret();
        this.clientName = map.getClientName();

        this.clientAuthenticationMethod = map.getClientAuthenticationMethod();
        this.scope = map.getScope();
        this.userNameAttributeName = map.getUserNameAttributeName();
        this.trustEmailAddress = map.getTrustEmailAddress();
        this.alwaysTrustEmailAddress = map.getAlwaysTrustEmailAddress();
        this.requireEmailAddress = map.getRequireEmailAddress();

        this.idAttribute = map.getIdAttribute();

        // explicit config
        this.authorizationUri = map.getAuthorizationUri();
        this.tokenUri = map.getTokenUri();
        this.jwkSetUri = map.getJwkSetUri();
        this.userInfoUri = map.getUserInfoUri();

        // autoconfiguration support from well-known
        this.issuerUri = map.getIssuerUri();

        // session
        this.propagateEndSession = map.getPropagateEndSession();
        this.respectTokenExpiration = map.getRespectTokenExpiration();
        this.promptMode = map.getPromptMode();

    }

    @JsonIgnore
    public static JsonSchema getConfigurationSchema() throws JsonMappingException {
        JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(mapper);
        return schemaGen.generateSchema(OIDCIdentityProviderConfigMap.class);
    }

}
