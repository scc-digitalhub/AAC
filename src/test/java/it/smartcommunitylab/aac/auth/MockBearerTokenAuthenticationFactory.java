package it.smartcommunitylab.aac.auth;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionAuthenticatedPrincipal;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

/*
 * A factory for building a mock security context from a mock authentication
 */
public class MockBearerTokenAuthenticationFactory
        implements WithSecurityContextFactory<WithMockBearerTokenAuthentication> {

    @Override
    public SecurityContext createSecurityContext(WithMockBearerTokenAuthentication annotation) {
        // build a new context
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        // map all authorities as-is
        // + map scopes as authorities with prefix
        Set<GrantedAuthority> authorities = Stream.concat(
                Stream.of(annotation.authorities())
                        .map(a -> new SimpleGrantedAuthority(a)),
                Stream.of(annotation.scopes())
                        .map(s -> new SimpleGrantedAuthority("SCOPE_" + s)))
                .collect(Collectors.toSet());

        // build principal with authorities
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("sub", annotation.subject());
        attrs.put("realm", annotation.realm());
        attrs.put("scopes", annotation.scopes());

        OAuth2IntrospectionAuthenticatedPrincipal principal = new OAuth2IntrospectionAuthenticatedPrincipal(attrs,
                authorities);

        // build a custom token
        OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, annotation.token(),
                null, null);

        // set authentication
        Authentication auth = new BearerTokenAuthentication(principal, token, authorities);
        context.setAuthentication(auth);

        return context;
    }

}
