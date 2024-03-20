package it.smartcommunitylab.aac.spid.auth;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.saml.auth.Saml2AuthenticationRequestRepository;
import it.smartcommunitylab.aac.saml.auth.SamlWebSsoAuthenticationRequestFilter;
import it.smartcommunitylab.aac.saml.auth.SerializableSaml2AuthenticationRequestContext;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProviderConfig;
import it.smartcommunitylab.aac.saml.service.HttpSessionSaml2AuthenticationRequestRepository;
import it.smartcommunitylab.aac.spid.SpidIdentityAuthority;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
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

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SpidWebSsoAuthenticationRequestFilter
    extends OncePerRequestFilter
    implements ApplicationEventPublisherAware {

    private static final String DEFAULT_FILTER_URI = SpidIdentityAuthority.AUTHORITY_URL + "authenticate/{registrationId}";
    private final String authorityId;
    private final RequestMatcher requestMatcher;

    private final Saml2AuthenticationRequestContextResolver authenticationRequestContextResolver;
    private final Saml2AuthenticationRequestFactory authenticationRequestFactory;
    private ApplicationEventPublisher eventPublisher;
    private final ProviderConfigRepository<SpidIdentityProviderConfig> registrationRepository;

    //TODO replace with spid specific implementation, handling resolution via relayState AND/OR requestId
    private Saml2AuthenticationRequestRepository<SerializableSaml2AuthenticationRequestContext> authenticationRequestRepository = new HttpSessionSaml2AuthenticationRequestRepository(
            HttpSessionSaml2AuthenticationRequestRepository.class.getName() + ".SPID_AUTHORIZATION_REQUEST");

    public SpidWebSsoAuthenticationRequestFilter(
        ProviderConfigRepository<SpidIdentityProviderConfig> registrationRepository,
        RelyingPartyRegistrationRepository relyingPartyRegistrationRepository
    ) {
        this(SystemKeys.AUTHORITY_SPID, registrationRepository, relyingPartyRegistrationRepository, DEFAULT_FILTER_URI);
    }

    public SpidWebSsoAuthenticationRequestFilter(
        String authorityId,
        ProviderConfigRepository<SpidIdentityProviderConfig> registrationRepository,
        RelyingPartyRegistrationRepository relyingPartyRegistrationRepository,
        String filterProcessingUrl
    ) {
        Assert.notNull(registrationRepository, "registration repository cannot be null");
        Assert.notNull(relyingPartyRegistrationRepository, "relying party repository cannot be null");

//        this.registrationRepository = registrationRepository;

        this.authorityId = authorityId;
        this.registrationRepository = registrationRepository;
        // use custom implementation to add secure relayState param
        this.authenticationRequestContextResolver = null; // TODO: review
        this.authenticationRequestFactory = null; // TODO: review
//        this.authenticationRequestContextResolver = new CustomSaml2AuthenticationRequestContextResolver(
//                new DefaultRelyingPartyRegistrationResolver(relyingPartyRegistrationRepository));
//        this.authenticationRequestFactory = getRequestFactory(registrationRepository);

        // set redirect to filterUrl
        this.requestMatcher = new AntPathRequestMatcher(filterProcessingUrl);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // TODO: nota che ci sono dei delta tra la versione vecchia saml e quella corrente
    }

    private void addParameter(String name, String value, UriComponentsBuilder builder) {
        Assert.hasText(name, "name cannot be empty or null");
        if (StringUtils.hasText(value)) {
            builder.queryParam(UriUtils.encode(name, StandardCharsets.ISO_8859_1),
                    UriUtils.encode(value, StandardCharsets.ISO_8859_1));
        }
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

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.eventPublisher = applicationEventPublisher;
    }

    // TODO: review
//    private class CustomSaml2AuthenticationRequestContextResolver
//            implements Saml2AuthenticationRequestContextResolver {
//
//        private final Converter<HttpServletRequest, RelyingPartyRegistration> relyingPartyRegistrationResolver;
//        private final StringKeyGenerator stateGenerator = new Base64StringKeyGenerator(Base64.getUrlEncoder());
//
//        public CustomSaml2AuthenticationRequestContextResolver(
//                Converter<HttpServletRequest, RelyingPartyRegistration> relyingPartyRegistrationResolver) {
//            this.relyingPartyRegistrationResolver = relyingPartyRegistrationResolver;
//        }
//
//        @Override
//        public Saml2AuthenticationRequestContext resolve(HttpServletRequest request) {
//            Assert.notNull(request, "request cannot be null");
//            RelyingPartyRegistration relyingParty = this.relyingPartyRegistrationResolver.convert(request);
//            if (relyingParty == null) {
//                return null;
//            }
//
//            return createRedirectAuthenticationRequestContext(request, relyingParty);
//        }
//
//        private Saml2AuthenticationRequestContext createRedirectAuthenticationRequestContext(HttpServletRequest request,
//                                                                                             RelyingPartyRegistration relyingParty) {
//
//            return Saml2AuthenticationRequestContext.builder().issuer(relyingParty.getEntityId())
//                    .relyingPartyRegistration(relyingParty)
//                    .assertionConsumerServiceUrl(relyingParty.getAssertionConsumerServiceLocation())
//                    .relayState(stateGenerator.generateKey()).build();
//        }
//    }
}