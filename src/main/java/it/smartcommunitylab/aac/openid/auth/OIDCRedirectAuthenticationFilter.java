package it.smartcommunitylab.aac.openid.auth;

import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;

import it.smartcommunitylab.aac.openid.OIDCIdentityAuthority;

/*
 * Build authorization request for external OIDC providers and redirects user-agent
 */

public class OIDCRedirectAuthenticationFilter extends OAuth2AuthorizationRequestRedirectFilter {

    public static final String DEFAULT_FILTER_URI = OIDCIdentityAuthority.AUTHORITY_URL + "authorize";

    public OIDCRedirectAuthenticationFilter(ClientRegistrationRepository clientRegistrationRepository) {
        this(clientRegistrationRepository, DEFAULT_FILTER_URI);
    }

    public OIDCRedirectAuthenticationFilter(ClientRegistrationRepository clientRegistrationRepository,
            String filterProcessesUrl) {

        // set PKCE aware request resolver to enforce pkce for both public and private
        // clients
        super(new PKCEAwareOAuth2AuthorizationRequestResolver(clientRegistrationRepository,
                filterProcessesUrl));
    }

}
