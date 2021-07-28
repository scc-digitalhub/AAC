package it.smartcommunitylab.aac.spid.auth;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.saml2.provider.service.metadata.Saml2MetadataResolver;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;
import it.smartcommunitylab.aac.core.provider.ProviderRepository;
import it.smartcommunitylab.aac.spid.SpidIdentityAuthority;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfig;

public class SpidMetadataFilter extends OncePerRequestFilter {

    public static final String DEFAULT_FILTER_URI = SpidIdentityAuthority.AUTHORITY_URL
            + "metadata/{registrationId}";

    private final RequestMatcher requestMatcher;

//    private final ProviderRepository<SpidIdentityProviderConfig> registrationRepository;
    private final Saml2MetadataResolver samlMetadataResolver;
    private final DefaultRelyingPartyRegistrationResolver registrationResolver;

    public SpidMetadataFilter(
            ProviderRepository<SpidIdentityProviderConfig> registrationRepository,
            RelyingPartyRegistrationRepository relyingPartyRegistrationRepository) {
        this(registrationRepository, relyingPartyRegistrationRepository, DEFAULT_FILTER_URI, null);
    }

    public SpidMetadataFilter(
            ProviderRepository<SpidIdentityProviderConfig> registrationRepository,
            RelyingPartyRegistrationRepository relyingPartyRegistrationRepository,
            String filterProcessingUrl, AuthenticationEntryPoint authenticationEntryPoint) {
        Assert.notNull(registrationRepository, "provider registration repository cannot be null");
        Assert.notNull(relyingPartyRegistrationRepository, "relyingPartyRegistrationRepository cannot be null");
        Assert.hasText(filterProcessingUrl, "filterProcessesUrl must contain a URL pattern");
        Assert.isTrue(filterProcessingUrl.contains("{registrationId}"),
                "filterProcessesUrl must contain a {registrationId} match variable");

//        this.registrationRepository = registrationRepository;

        // we need to build a custom requestMatcher to extract variables from url
        this.requestMatcher = new AntPathRequestMatcher(filterProcessingUrl);

        // use default request resolver
        this.registrationResolver = new DefaultRelyingPartyRegistrationResolver(relyingPartyRegistrationRepository);

        // use our custom meta resolver
        this.samlMetadataResolver = new SpidSamlMetadataResolver(registrationRepository);

    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        RequestMatcher.MatchResult matcher = this.requestMatcher.matcher(request);
        if (!matcher.isMatch()) {
            chain.doFilter(request, response);
            return;
        }

        String registrationId = matcher.getVariables().get("registrationId");
//        // fetch config and load meta registration, we build all meta identical
//        SpidIdentityProviderConfig providerConfig = registrationRepository.findByProviderId(registrationId);
//
//        if (providerConfig == null) {
//            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//            return;
//        }

        RelyingPartyRegistration relyingPartyRegistration = registrationResolver.convert(request);
        if (relyingPartyRegistration == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String metadata = samlMetadataResolver.resolve(relyingPartyRegistration);

        // write response as xml
        response.setContentType(MediaType.APPLICATION_XML_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"saml-" + registrationId + "-metadata.xml\"");
        response.setContentLength(metadata.length());
        response.getWriter().write(metadata);
    }

}
