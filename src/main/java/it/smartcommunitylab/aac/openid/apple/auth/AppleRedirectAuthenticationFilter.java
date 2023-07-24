package it.smartcommunitylab.aac.openid.apple.auth;

import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.openid.apple.AppleIdentityAuthority;
import it.smartcommunitylab.aac.openid.apple.provider.AppleIdentityProviderConfig;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;

/*
 * Build authorization request for Apple
 *
 * Note: use a custom filter to make sure oncePerRequest uses our name to check execution
 */

public class AppleRedirectAuthenticationFilter extends OAuth2AuthorizationRequestRedirectFilter {

    public static final String DEFAULT_FILTER_URI = AppleIdentityAuthority.AUTHORITY_URL + "authorize";

    public AppleRedirectAuthenticationFilter(
        ProviderConfigRepository<AppleIdentityProviderConfig> registrationRepository,
        ClientRegistrationRepository clientRegistrationRepository
    ) {
        this(registrationRepository, clientRegistrationRepository, DEFAULT_FILTER_URI);
    }

    public AppleRedirectAuthenticationFilter(
        ProviderConfigRepository<AppleIdentityProviderConfig> registrationRepository,
        ClientRegistrationRepository clientRegistrationRepository,
        String filterProcessesUrl
    ) {
        super(clientRegistrationRepository, filterProcessesUrl);
    }
}
