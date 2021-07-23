package it.smartcommunitylab.aac.saml.auth;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.saml2.provider.service.authentication.OpenSamlAuthenticationProvider.ResponseToken;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;

public class SamlAuthentication extends AbstractAuthenticationToken {

    private static final long serialVersionUID = SystemKeys.AAC_SAML_SERIAL_VERSION;

    private final AuthenticatedPrincipal principal;

    private final ResponseToken responseToken;

    public SamlAuthentication(AuthenticatedPrincipal principal, ResponseToken responseToken,
            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        Assert.notNull(principal, "principal cannot be null");
        Assert.notNull(responseToken, "responseToken cannot be null");
        this.principal = principal;
        this.responseToken = responseToken;
        setAuthenticated(true);
    }

    @Override
    public AuthenticatedPrincipal getPrincipal() {
        return this.principal;
    }

    @Override
    public Object getCredentials() {
        return getSaml2Response();
    }

    public ResponseToken getResponseToken() {
        return responseToken;
    }

    public String getSaml2Response() {
        return responseToken.getToken().getSaml2Response();
    }

}
