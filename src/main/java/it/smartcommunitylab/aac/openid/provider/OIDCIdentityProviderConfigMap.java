package it.smartcommunitylab.aac.openid.provider;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import org.apache.logging.log4j.CloseableThreadContext.Instance;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.core.base.ConfigurableProperties;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class OIDCIdentityProviderConfigMap implements ConfigurableProperties {

    private static ObjectMapper mapper = new ObjectMapper();
    private final static TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<HashMap<String, Serializable>>() {
    };

    @NotBlank
    private String clientId;
    private String clientSecret;
    private String clientName;

    private ClientAuthenticationMethod clientAuthenticationMethod;
    private String scope;
    private String userNameAttributeName = "sub";

    // explicit config
    private String authorizationUri;
    private String tokenUri;
    private String jwkSetUri;
    private String userInfoUri;

    // autoconfiguration support from well-known
    private String issuerUri;

    public OIDCIdentityProviderConfigMap() {
        this.scope = "openid,profile,email";

        this.clientAuthenticationMethod = ClientAuthenticationMethod.BASIC;
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

    public ClientAuthenticationMethod getClientAuthenticationMethod() {
        return clientAuthenticationMethod;
    }

    public void setClientAuthenticationMethod(ClientAuthenticationMethod clientAuthenticationMethod) {
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

    @Override
    @JsonIgnore
    public Map<String, Serializable> getConfiguration() {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        return mapper.convertValue(this, typeRef);
    }

    @SuppressWarnings("rawtypes")
	@Override
    @JsonIgnore
    public void setConfiguration(Map<String, Serializable> props) {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        // workaround for clientAuthenticationMethod not having default constructor
        ClientAuthenticationMethod method = null;
        if (props.containsKey("clientAuthenticationMethod")) {
        	Object v = props.get("clientAuthenticationMethod");
        	String name = v instanceof String ? v.toString() : (String)((Map)v).get("value");
        	method = new ClientAuthenticationMethod(name);
        	props.remove("clientAuthenticationMethod");
        }
        
        OIDCIdentityProviderConfigMap map = mapper.convertValue(props, OIDCIdentityProviderConfigMap.class);
        map.setClientAuthenticationMethod(method);

        this.clientId = map.getClientId();
        this.clientSecret = map.getClientSecret();
        this.clientName = map.getClientName();

        this.clientAuthenticationMethod = map.getClientAuthenticationMethod();
        this.scope = map.getScope();
        this.userNameAttributeName = map.getUserNameAttributeName();

        // explicit config
        this.authorizationUri = map.getAuthorizationUri();
        this.tokenUri = map.getTokenUri();
        this.jwkSetUri = map.getJwkSetUri();
        this.userInfoUri = map.getUserInfoUri();

        // autoconfiguration support from well-known
        this.issuerUri = map.getIssuerUri();

    }

}
