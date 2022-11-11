package it.smartcommunitylab.aac.auth;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.annotation.AliasFor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionAuthenticatedPrincipal;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;

/*
 * A factory for building a mock security context from a mock authentication
 */
public class MockBearerTokenAuthenticationFactory
        implements WithSecurityContextFactory<WithMockBearerTokenAuthentication> {

    @Override
    public SecurityContext createSecurityContext(WithMockBearerTokenAuthentication annotation) {
        // explode annotation
        MockBearerTokenAuthentication token = new MockBearerTokenAuthentication(annotation.subject());
        token.setRealm(annotation.realm());
        token.setAuthorities(annotation.authorities());
        token.setScopes(annotation.scopes());
        token.setToken(annotation.token());

        // build authentication
        Authentication auth = createAuthentication(token);

        // build a new context
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);

        return context;
    }

    public static Authentication createAuthentication(MockBearerTokenAuthentication token) {
        // map all authorities as-is
        // + map scopes as authorities with prefix
        Set<GrantedAuthority> authorities = Stream.concat(
                Stream.of(token.getAuthorities())
                        .map(a -> new SimpleGrantedAuthority(a)),
                Stream.of(token.getScopes())
                        .map(s -> new SimpleGrantedAuthority("SCOPE_" + s)))
                .collect(Collectors.toSet());

        // build principal with authorities
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("sub", token.getSubject());
        attrs.put("realm", token.getRealm());
        attrs.put("scopes", token.getScopes());

        OAuth2IntrospectionAuthenticatedPrincipal principal = new OAuth2IntrospectionAuthenticatedPrincipal(attrs,
                authorities);

        // build a custom token
        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, token.getToken(),
                null, null);

        // set authentication
        Authentication auth = new BearerTokenAuthentication(principal, accessToken, authorities);
        return auth;
    }

    public static final class MockBearerTokenAuthentication {
        private String subject;

        private String realm;

        private String[] authorities;

        private String[] scopes;

        private String token;

        public MockBearerTokenAuthentication(String subject) {
            this.subject = subject;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getRealm() {
            return realm;
        }

        public void setRealm(String realm) {
            this.realm = realm;
        }

        public String[] getAuthorities() {
            return authorities;
        }

        public void setAuthorities(String[] authorities) {
            this.authorities = authorities;
        }

        public String[] getScopes() {
            return scopes;
        }

        public void setScopes(String[] scopes) {
            this.scopes = scopes;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

    }
}
