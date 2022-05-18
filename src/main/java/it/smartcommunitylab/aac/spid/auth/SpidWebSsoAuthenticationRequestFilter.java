package it.smartcommunitylab.aac.spid.auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationRequestContext;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationRequestFactory;
import org.springframework.security.saml2.provider.service.authentication.Saml2PostAuthenticationRequest;
import org.springframework.security.saml2.provider.service.authentication.Saml2RedirectAuthenticationRequest;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationRequestContextResolver;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.saml.auth.Saml2AuthenticationRequestRepository;
import it.smartcommunitylab.aac.saml.service.HttpSessionSaml2AuthenticationRequestRepository;
import it.smartcommunitylab.aac.spid.SpidIdentityAuthority;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfig;

public class SpidWebSsoAuthenticationRequestFilter extends OncePerRequestFilter {

    public static final String DEFAULT_FILTER_URI = SpidIdentityAuthority.AUTHORITY_URL
            + "authenticate/{registrationId}";

    private final RequestMatcher requestMatcher;

    private final Saml2AuthenticationRequestContextResolver authenticationRequestContextResolver;
    private final Saml2AuthenticationRequestFactory authenticationRequestFactory;
//    private final ProviderRepository<SamlIdentityProviderConfig> registrationRepository;

    //TODO replace with spid specific implementation, handling resolution via relayState AND/OR requestId
    private Saml2AuthenticationRequestRepository<Saml2AuthenticationRequestContext> authenticationRequestRepository = new HttpSessionSaml2AuthenticationRequestRepository(
            HttpSessionSaml2AuthenticationRequestRepository.class.getName() + ".SPID_AUTHORIZATION_REQUEST");

    public SpidWebSsoAuthenticationRequestFilter(
            ProviderConfigRepository<SpidIdentityProviderConfig> registrationRepository,
            RelyingPartyRegistrationRepository relyingPartyRegistrationRepository) {
        this(registrationRepository, relyingPartyRegistrationRepository, DEFAULT_FILTER_URI);
    }

    public SpidWebSsoAuthenticationRequestFilter(
            ProviderConfigRepository<SpidIdentityProviderConfig> registrationRepository,
            RelyingPartyRegistrationRepository relyingPartyRegistrationRepository,
            String filterProcessingUrl) {
        Assert.notNull(registrationRepository, "registration repository cannot be null");
        Assert.notNull(relyingPartyRegistrationRepository, "relying party repository cannot be null");

//        this.registrationRepository = registrationRepository;

        // use custom implementation to add secure relayState param
        this.authenticationRequestContextResolver = new CustomSaml2AuthenticationRequestContextResolver(
                new DefaultRelyingPartyRegistrationResolver(relyingPartyRegistrationRepository));
        this.authenticationRequestFactory = getRequestFactory(registrationRepository);

        // set redirect to filterUrl
        this.requestMatcher = new AntPathRequestMatcher(filterProcessingUrl);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!requestMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Saml2AuthenticationRequestContext context = this.authenticationRequestContextResolver.resolve(request);
        if (context == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // persist request if relayState is set
        if (StringUtils.hasText(context.getRelayState())) {
            authenticationRequestRepository.saveAuthenticationRequest(context, request, response);
        }

        RelyingPartyRegistration relyingParty = context.getRelyingPartyRegistration();
        if (relyingParty.getAssertingPartyDetails().getSingleSignOnServiceBinding() == Saml2MessageBinding.REDIRECT) {
            sendRedirect(response, context);
        } else {
            sendPost(response, context);
        }
    }

    private void sendRedirect(HttpServletResponse response, Saml2AuthenticationRequestContext context)
            throws IOException {
        Saml2RedirectAuthenticationRequest authenticationRequest = this.authenticationRequestFactory
                .createRedirectAuthenticationRequest(context);
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromUriString(authenticationRequest.getAuthenticationRequestUri());
        addParameter("SAMLRequest", authenticationRequest.getSamlRequest(), uriBuilder);
        addParameter("RelayState", authenticationRequest.getRelayState(), uriBuilder);
        addParameter("SigAlg", authenticationRequest.getSigAlg(), uriBuilder);
        addParameter("Signature", authenticationRequest.getSignature(), uriBuilder);
        String redirectUrl = uriBuilder.build(true).toUriString();
        response.sendRedirect(redirectUrl);
    }

    private void addParameter(String name, String value, UriComponentsBuilder builder) {
        Assert.hasText(name, "name cannot be empty or null");
        if (StringUtils.hasText(value)) {
            builder.queryParam(UriUtils.encode(name, StandardCharsets.ISO_8859_1),
                    UriUtils.encode(value, StandardCharsets.ISO_8859_1));
        }
    }

    private void sendPost(HttpServletResponse response, Saml2AuthenticationRequestContext context) throws IOException {
        Saml2PostAuthenticationRequest authenticationRequest = this.authenticationRequestFactory
                .createPostAuthenticationRequest(context);
        String html = createSamlPostRequestFormData(authenticationRequest);
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.getWriter().write(html);
    }

    private String createSamlPostRequestFormData(Saml2PostAuthenticationRequest authenticationRequest) {
        String authenticationRequestUri = authenticationRequest.getAuthenticationRequestUri();
        String relayState = authenticationRequest.getRelayState();
        String samlRequest = authenticationRequest.getSamlRequest();
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n").append("    <head>\n");
        html.append("        <meta charset=\"utf-8\" />\n");
        html.append("    </head>\n");
        html.append("    <body onload=\"document.forms[0].submit()\">\n");
        html.append("        <noscript>\n");
        html.append("            <p>\n");
        html.append("                <strong>Note:</strong> Since your browser does not support JavaScript,\n");
        html.append("                you must press the Continue button once to proceed.\n");
        html.append("            </p>\n");
        html.append("        </noscript>\n");
        html.append("        \n");
        html.append("        <form action=\"");
        html.append(authenticationRequestUri);
        html.append("\" method=\"post\">\n");
        html.append("            <div>\n");
        html.append("                <input type=\"hidden\" name=\"SAMLRequest\" value=\"");
        html.append(HtmlUtils.htmlEscape(samlRequest));
        html.append("\"/>\n");
        if (StringUtils.hasText(relayState)) {
            html.append("                <input type=\"hidden\" name=\"RelayState\" value=\"");
            html.append(HtmlUtils.htmlEscape(relayState));
            html.append("\"/>\n");
        }
        html.append("            </div>\n");
        html.append("            <noscript>\n");
        html.append("                <div>\n");
        html.append("                    <input type=\"submit\" value=\"Continue\"/>\n");
        html.append("                </div>\n");
        html.append("            </noscript>\n");
        html.append("        </form>\n");
        html.append("        \n");
        html.append("    </body>\n");
        html.append("</html>");
        return html.toString();
    }

    private static Saml2AuthenticationRequestFactory getRequestFactory(
            ProviderConfigRepository<SpidIdentityProviderConfig> registrationRepository) {
        org.springframework.security.saml2.provider.service.authentication.OpenSamlAuthenticationRequestFactory factory = new org.springframework.security.saml2.provider.service.authentication.OpenSamlAuthenticationRequestFactory();
        factory.setAuthenticationRequestContextConverter(
                new SpidAuthenticationRequestContextConverter(registrationRepository));

        return factory;
    }

    public void setAuthenticationRequestRepository(
            Saml2AuthenticationRequestRepository<Saml2AuthenticationRequestContext> authenticationRequestRepository) {
        this.authenticationRequestRepository = authenticationRequestRepository;
    }

    private class CustomSaml2AuthenticationRequestContextResolver
            implements Saml2AuthenticationRequestContextResolver {

        private final Converter<HttpServletRequest, RelyingPartyRegistration> relyingPartyRegistrationResolver;
        private final StringKeyGenerator stateGenerator = new Base64StringKeyGenerator(Base64.getUrlEncoder());

        public CustomSaml2AuthenticationRequestContextResolver(
                Converter<HttpServletRequest, RelyingPartyRegistration> relyingPartyRegistrationResolver) {
            this.relyingPartyRegistrationResolver = relyingPartyRegistrationResolver;
        }

        @Override
        public Saml2AuthenticationRequestContext resolve(HttpServletRequest request) {
            Assert.notNull(request, "request cannot be null");
            RelyingPartyRegistration relyingParty = this.relyingPartyRegistrationResolver.convert(request);
            if (relyingParty == null) {
                return null;
            }

            return createRedirectAuthenticationRequestContext(request, relyingParty);
        }

        private Saml2AuthenticationRequestContext createRedirectAuthenticationRequestContext(HttpServletRequest request,
                RelyingPartyRegistration relyingParty) {

            return Saml2AuthenticationRequestContext.builder().issuer(relyingParty.getEntityId())
                    .relyingPartyRegistration(relyingParty)
                    .assertionConsumerServiceUrl(relyingParty.getAssertionConsumerServiceLocation())
                    .relayState(stateGenerator.generateKey()).build();
        }
    }
}