package it.smartcommunitylab.aac.oauth.auth;

import it.smartcommunitylab.aac.SystemKeys;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

public class OAuth2ClientJwtAssertionAuthenticationToken extends OAuth2ClientAuthenticationToken {

    private static final long serialVersionUID = SystemKeys.AAC_OAUTH2_SERIAL_VERSION;

    public static final String CLIENT_ASSERTION_TYPE_VALUE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

    private String clientAssertion;

    public OAuth2ClientJwtAssertionAuthenticationToken(
        String clientId,
        String clientAssertion,
        String authenticationMethod
    ) {
        super(clientId);
        this.clientAssertion = clientAssertion;
        this.authenticationMethod = authenticationMethod;
        setAuthenticated(false);
    }

    public OAuth2ClientJwtAssertionAuthenticationToken(
        String clientId,
        String clientAssertion,
        String authenticationMethod,
        Collection<? extends GrantedAuthority> authorities
    ) {
        super(clientId, authorities);
        this.clientAssertion = clientAssertion;
        this.authenticationMethod = authenticationMethod;
    }

    public String getClientAssertion() {
        return clientAssertion;
    }

    @Override
    public String getCredentials() {
        return this.clientAssertion;
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        this.clientAssertion = null;
    }
}
