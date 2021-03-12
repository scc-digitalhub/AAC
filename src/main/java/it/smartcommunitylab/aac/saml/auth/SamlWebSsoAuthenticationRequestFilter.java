package it.smartcommunitylab.aac.saml.auth;

import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.servlet.filter.Saml2WebSsoAuthenticationRequestFilter;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.DefaultSaml2AuthenticationRequestContextResolver;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import it.smartcommunitylab.aac.saml.SamlIdentityAuthority;

public class SamlWebSsoAuthenticationRequestFilter extends Saml2WebSsoAuthenticationRequestFilter {

    public static final String DEFAULT_FILTER_URI = SamlIdentityAuthority.AUTHORITY_URL
            + "authenticate/{registrationId}";

//    private final Saml2AuthenticationRequestContextResolver authenticationRequestContextResolver;
//    private final Saml2AuthenticationRequestFactory authenticationRequestFactory;

    public SamlWebSsoAuthenticationRequestFilter(
            RelyingPartyRegistrationRepository relyingPartyRegistrationRepository) {
        this(relyingPartyRegistrationRepository, DEFAULT_FILTER_URI);
    }

    public SamlWebSsoAuthenticationRequestFilter(RelyingPartyRegistrationRepository relyingPartyRegistrationRepository,
            String filterProcessingUrl) {
        // build defaults to feed super
        // TODO rewrite to fetch these from external
        super(new DefaultSaml2AuthenticationRequestContextResolver(
                new DefaultRelyingPartyRegistrationResolver(relyingPartyRegistrationRepository)),
                new org.springframework.security.saml2.provider.service.authentication.OpenSamlAuthenticationRequestFactory());

        // set redirect to filterUrl
        RequestMatcher redirectMatcher = new AntPathRequestMatcher(filterProcessingUrl);
        this.setRedirectMatcher(redirectMatcher);
    }

}
