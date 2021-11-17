package it.smartcommunitylab.aac.saml.auth;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.saml2.provider.service.metadata.OpenSamlMetadataResolver;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.Saml2MetadataFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import it.smartcommunitylab.aac.saml.SamlIdentityAuthority;

public class SamlMetadataFilter extends OncePerRequestFilter {

    public static final String DEFAULT_FILTER_URI = SamlIdentityAuthority.AUTHORITY_URL
            + "metadata/{registrationId}";

    private final Saml2MetadataFilter samlMetadataFilter;

    public SamlMetadataFilter(RelyingPartyRegistrationRepository relyingPartyRegistrationRepository) {
        this(relyingPartyRegistrationRepository, DEFAULT_FILTER_URI);
    }

    public SamlMetadataFilter(RelyingPartyRegistrationRepository relyingPartyRegistrationRepository,
            String filterProcessingUrl) {

        // build a converter and a resolver for the filter
        Converter<HttpServletRequest, RelyingPartyRegistration> relyingPartyRegistrationResolver = new DefaultRelyingPartyRegistrationResolver(
                relyingPartyRegistrationRepository);
        
        samlMetadataFilter = new Saml2MetadataFilter(relyingPartyRegistrationResolver,
                new OpenSamlMetadataResolver());
        
        RequestMatcher requestMatcher = new AntPathRequestMatcher(filterProcessingUrl, "GET");
        samlMetadataFilter.setRequestMatcher(requestMatcher);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // delegate
        samlMetadataFilter.doFilter(request, response, filterChain);

    }

}
