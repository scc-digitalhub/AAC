package it.smartcommunitylab.aac.saml.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.core.Saml2ErrorCodes;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationTokenConverter;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.ProviderWrappedAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.UserAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;
import it.smartcommunitylab.aac.saml.SamlIdentityAuthority;

public class SamlWebSsoAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    public static final String DEFAULT_FILTER_URI = SamlIdentityAuthority.AUTHORITY_URL + "sso/{registrationId}";

    private final Saml2AuthenticationTokenConverter authenticationConverter;

    public SamlWebSsoAuthenticationFilter(RelyingPartyRegistrationRepository relyingPartyRegistrationRepository) {
        this(relyingPartyRegistrationRepository, DEFAULT_FILTER_URI);
    }

    public SamlWebSsoAuthenticationFilter(RelyingPartyRegistrationRepository relyingPartyRegistrationRepository,
            String filterProcessingUrl) {
        super(filterProcessingUrl);
        Assert.notNull(relyingPartyRegistrationRepository, "relyingPartyRegistrationRepository cannot be null");
        Assert.hasText(filterProcessingUrl, "filterProcessesUrl must contain a URL pattern");
        Assert.isTrue(filterProcessingUrl.contains("{registrationId}"),
                "filterProcessesUrl must contain a {registrationId} match variable");

        // build a default resolver, will lookup by registrationId
        DefaultRelyingPartyRegistrationResolver registrationResolver = new DefaultRelyingPartyRegistrationResolver(
                relyingPartyRegistrationRepository);
        // use the default token converter
        authenticationConverter = new Saml2AuthenticationTokenConverter(registrationResolver);

        // enforce session id change to prevent fixation attacks
        setAllowSessionCreation(true);
        setSessionAuthenticationStrategy(new ChangeSessionIdAuthenticationStrategy());
    }

    @Override
    protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
        return (super.requiresAuthentication(request, response)
                && StringUtils.hasText(request.getParameter("SAMLResponse")));
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        // use converter to fetch rpRegistration and parse saml response
        Saml2AuthenticationToken authenticationRequest = authenticationConverter.convert(request);
        if (authenticationRequest == null) {
            Saml2Error saml2Error = new Saml2Error(
                    Saml2ErrorCodes.RELYING_PARTY_REGISTRATION_NOT_FOUND,
                    "No relying party registration found");
            throw new Saml2AuthenticationException(saml2Error);
        }

        // fetch rp registration
        RelyingPartyRegistration registration = authenticationRequest.getRelyingPartyRegistration();
        String registrationId = registration.getRegistrationId();

        // collect info for webauth as additional details
        Object authenticationDetails = this.authenticationDetailsSource.buildDetails(request);

        // wrap auth request for multi-provider manager
        // providerId is registrationId
        String providerId = registrationId;
        ProviderWrappedAuthenticationToken wrappedAuthRequest = new ProviderWrappedAuthenticationToken(
                authenticationRequest,
                providerId, SystemKeys.AUTHORITY_SAML);

        // also collect request details
        WebAuthenticationDetails webAuthenticationDetails = new WebAuthenticationDetails(request);
        wrappedAuthRequest.setAuthenticationDetails(webAuthenticationDetails);

        // authenticate via extended authManager
        UserAuthenticationToken userAuthentication = (UserAuthenticationToken) getAuthenticationManager()
                .authenticate(wrappedAuthRequest);

        // return authentication to be set in security context
        return userAuthentication;
    }

}
