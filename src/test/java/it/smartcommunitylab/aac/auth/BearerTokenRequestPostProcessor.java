package it.smartcommunitylab.aac.auth;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.auth.MockBearerTokenAuthenticationFactory.MockBearerTokenAuthentication;

public final class BearerTokenRequestPostProcessor implements RequestPostProcessor {

    private String tokenValue;
    private String subject = "00000000-0000-0000-0000-000000000000";
    private String realm = SystemKeys.REALM_SYSTEM;

    private String[] authorities = { Config.R_USER };
    private String[] scopes = {};

    private BearerTokenRequestPostProcessor(String token) {
        this.tokenValue = token;
    }

    private BearerTokenRequestPostProcessor(String token, String subject) {
        this.tokenValue = token;
        this.subject = subject;
    }

    public BearerTokenRequestPostProcessor subject(String subject) {
        this.subject = subject;
        return this;
    }

    public BearerTokenRequestPostProcessor realm(String realm) {
        this.realm = realm;
        return this;
    }

    public BearerTokenRequestPostProcessor authorities(String[] authorities) {
        this.authorities = authorities;
        return this;
    }

    public BearerTokenRequestPostProcessor scopes(String[] scopes) {
        this.scopes = scopes;
        return this;
    }

    @Override
    public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
        CsrfFilter.skipRequest(request);

        // add as header
        request.addHeader("Authorization", this.tokenValue);

        // also resolve to a new security context
        // leverage bearer factory
        MockBearerTokenAuthentication token = new MockBearerTokenAuthentication(subject);
        token.setRealm(realm);
        token.setAuthorities(authorities);
        token.setScopes(scopes);
        token.setToken(tokenValue);

        Authentication auth = MockBearerTokenAuthenticationFactory.createAuthentication(token);

        // use authentication to properly inject into test security context
        return SecurityMockMvcRequestPostProcessors.authentication(auth).postProcessRequest(request);

    }

    public static BearerTokenRequestPostProcessor bearer(String token) {
        return new BearerTokenRequestPostProcessor(token);
    }
}
