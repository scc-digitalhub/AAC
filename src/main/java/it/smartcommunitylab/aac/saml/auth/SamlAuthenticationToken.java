package it.smartcommunitylab.aac.saml.auth;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;

public class SamlAuthenticationToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = SystemKeys.AAC_SAML_SERIAL_VERSION;

    private final String subject;

    private final Saml2AuthenticatedPrincipal principal;

    private final String saml2Response;
//    private final transient ResponseToken responseToken;

    public SamlAuthenticationToken(String subject, Saml2AuthenticatedPrincipal principal, String saml2Response,
            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        Assert.hasText(subject, "subject cannot be null or empty");
        Assert.notNull(principal, "principal cannot be null");
        Assert.hasText(saml2Response, "saml2Response cannot be null or empty");
        this.subject = subject;
        this.principal = principal;
        this.saml2Response = saml2Response;
//        this.saml2Response = responseToken.getToken().getSaml2Response();
//        this.responseToken = responseToken;
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

    public String getSubject() {
        return subject;
    }

//    @JsonIgnore
//    public ResponseToken getResponseToken() {
//        return responseToken;
//    }

    public String getSaml2Response() {
        return saml2Response;
    }

}
