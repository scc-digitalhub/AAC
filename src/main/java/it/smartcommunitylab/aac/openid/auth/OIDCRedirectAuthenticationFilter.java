package it.smartcommunitylab.aac.openid.auth;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.core.provider.ProviderRepository;
import it.smartcommunitylab.aac.openid.OIDCIdentityAuthority;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProviderConfig;

/*
 * Build authorization request for external OIDC providers and redirects user-agent
 */

public class OIDCRedirectAuthenticationFilter extends OAuth2AuthorizationRequestRedirectFilter {

    public static final String DEFAULT_FILTER_URI = OIDCIdentityAuthority.AUTHORITY_URL + "authorize";

    // we need to load client registration
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final ProviderRepository<OIDCIdentityProviderConfig> registrationRepository;

    public OIDCRedirectAuthenticationFilter(ProviderRepository<OIDCIdentityProviderConfig> registrationRepository,
            ClientRegistrationRepository clientRegistrationRepository) {
        this(registrationRepository, clientRegistrationRepository, DEFAULT_FILTER_URI);
    }

    public OIDCRedirectAuthenticationFilter(ProviderRepository<OIDCIdentityProviderConfig> registrationRepository,
            ClientRegistrationRepository clientRegistrationRepository,
            String filterProcessesUrl) {

        // set PKCE aware request resolver to enforce pkce for both public and private
        // clients
        super(new ExtendedAuthorizationRequestResolver(
                new PKCEAwareOAuth2AuthorizationRequestResolver(clientRegistrationRepository,
                        filterProcessesUrl),
                registrationRepository));

        Assert.notNull(registrationRepository, "provider registration repository cannot be null");
        Assert.notNull(clientRegistrationRepository, "clientRegistrationRepository cannot be null");

        this.registrationRepository = registrationRepository;
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

}
