/*
 * Copyright 2024 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.spid.auth;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.saml.auth.Saml2AuthenticationRequestRepository;
import it.smartcommunitylab.aac.saml.auth.SerializableSaml2AuthenticationRequestContext;
import it.smartcommunitylab.aac.saml.events.SamlAuthenticationRequestEvent;
import it.smartcommunitylab.aac.saml.service.HttpSessionSaml2AuthenticationRequestRepository;
import it.smartcommunitylab.aac.spid.SpidIdentityAuthority;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.saml2.provider.service.authentication.*;
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

/*
 * SpidWebSsoAuthenticationRequestFilter is the filter that intercepts SPID authentication requests, generates the SAML
 * request and then sends the sso authentication request to the selected (SPID) SAML identity provider.
 * This is a filter accordingly with the Spring Security architecture.
 * For more on how SPID authentication requests are made, see
 *  https://docs.italia.it/italia/spid/spid-regole-tecniche/it/stabile/single-sign-on.html
 * For more on the Spring Security architecture and why an AuthenticationRequestFilter is required, see
 *  https://docs.spring.io/spring-security/reference/servlet/saml2/login/overview.html
 */
public class SpidWebSsoAuthenticationRequestFilter
    extends OncePerRequestFilter
    implements ApplicationEventPublisherAware {

    private static final String DEFAULT_FILTER_URI =
        SpidIdentityAuthority.AUTHORITY_URL + "authenticate/{registrationId}"; // NOTE: if multiple upstream idps are possible, then registration id must somehow also encode which idp is target of the authentication request
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String authorityId;
    private final RequestMatcher requestMatcher;

    private final Saml2AuthenticationRequestContextResolver authenticationRequestContextResolver;
    private final Saml2AuthenticationRequestFactory authenticationRequestFactory;
    private ApplicationEventPublisher eventPublisher;
    private final ProviderConfigRepository<SpidIdentityProviderConfig> providerConfigRepository;

    //TODO replace with spid specific implementation, handling resolution via relayState AND/OR requestId
    private Saml2AuthenticationRequestRepository<SerializableSaml2AuthenticationRequestContext> authenticationRequestRepository =
        new HttpSessionSaml2AuthenticationRequestRepository(
            HttpSessionSaml2AuthenticationRequestRepository.class.getName() + ".SPID_AUTHORIZATION_REQUEST"
        );

    public SpidWebSsoAuthenticationRequestFilter(
        ProviderConfigRepository<SpidIdentityProviderConfig> providerConfigRepository,
        RelyingPartyRegistrationRepository relyingPartyRegistrationRepository
    ) {
        this(
            SystemKeys.AUTHORITY_SPID,
            providerConfigRepository,
            relyingPartyRegistrationRepository,
            DEFAULT_FILTER_URI
        );
    }

    public SpidWebSsoAuthenticationRequestFilter(
        String authorityId,
        ProviderConfigRepository<SpidIdentityProviderConfig> providerConfigRepository,
        RelyingPartyRegistrationRepository relyingPartyRegistrationRepository,
        String filterProcessingUrl
    ) {
        Assert.notNull(providerConfigRepository, "registration repository cannot be null");
        Assert.notNull(relyingPartyRegistrationRepository, "relying party repository cannot be null");
        Assert.notNull(relyingPartyRegistrationRepository, "relying party repository cannot be null");

        this.authorityId = authorityId;
        this.providerConfigRepository = providerConfigRepository;
        // use custom implementation to add secure relayState param
        this.authenticationRequestContextResolver =
            new CustomSpidAuthenticationRequestContextResolver(
                new DefaultRelyingPartyRegistrationResolver(relyingPartyRegistrationRepository)
            );
        this.authenticationRequestFactory = getRequestFactory(providerConfigRepository);

        // set redirect to filterUrl
        this.requestMatcher = new AntPathRequestMatcher(filterProcessingUrl);
    }

    private static Saml2AuthenticationRequestFactory getRequestFactory(
        ProviderConfigRepository<SpidIdentityProviderConfig> registrationRepository
    ) {
        org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationRequestFactory factory =
            new org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationRequestFactory();
        factory.setAuthenticationRequestContextConverter(
            new SpidAuthenticationRequestContextConverter(registrationRepository)
        );

        return factory;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        if (!requestMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        logger.debug("resolving context from http request");
        Saml2AuthenticationRequestContext context = this.authenticationRequestContextResolver.resolve(request);
        if (context == null) {
            logger.debug("error resolving context from request");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        //resolve provider - registrationId is {providerId}|{idpKey}
        String registrationId = context.getRelyingPartyRegistration().getRegistrationId();
        SpidIdentityProviderConfig config = providerConfigRepository.findByProviderId(
            SpidIdentityProviderConfig.getProviderId(registrationId)
        );
        if (config == null) {
            logger.error("error retrieving provider for registration {}", registrationId);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        // translate context to a serializable version
        // TODO drop and adopt resolver+request as per spring 5.6+
        AbstractSaml2AuthenticationRequest authenticationRequest = resolve(context);

        String authRequestId = SpidIdentityProviderConfig.getProviderId(
            context.getRelyingPartyRegistration().getRegistrationId()
        ); // must be matched by the response calling the sso url

        SerializableSaml2AuthenticationRequestContext ctx = new SerializableSaml2AuthenticationRequestContext(
            authRequestId,
            context.getIssuer(),
            context.getRelayState(),
            authenticationRequest
        );

        logger.debug("resolved context relayState: {}", ctx.getRelayState());

        if (logger.isTraceEnabled()) {
            logger.trace("context: {}", String.valueOf(ctx));
        }

        // persist request if relayState is set
        if (StringUtils.hasText(context.getRelayState())) {
            logger.debug("persisting authentication request with context under key: {}", ctx.getRelayState());
            authenticationRequestRepository.saveAuthenticationRequest(ctx, request, response);
        }

        if (eventPublisher != null) {
            eventPublisher.publishEvent(
                new SamlAuthenticationRequestEvent(
                    authorityId,
                    config.getProvider(),
                    config.getRealm(),
                    authenticationRequest
                )
            );
        }

        if (authenticationRequest instanceof Saml2RedirectAuthenticationRequest) {
            sendRedirect(response, (Saml2RedirectAuthenticationRequest) authenticationRequest);
        } else {
            sendPost(response, (Saml2PostAuthenticationRequest) authenticationRequest);
        }
    }

    private void sendRedirect(HttpServletResponse response, Saml2RedirectAuthenticationRequest authenticationRequest)
        throws IOException {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(
            authenticationRequest.getAuthenticationRequestUri()
        );
        addParameter("SAMLRequest", authenticationRequest.getSamlRequest(), uriBuilder);
        addParameter("RelayState", authenticationRequest.getRelayState(), uriBuilder);
        addParameter("SigAlg", authenticationRequest.getSigAlg(), uriBuilder);
        addParameter("Signature", authenticationRequest.getSignature(), uriBuilder);
        String redirectUrl = uriBuilder.build(true).toUriString();

        logger.info("send redirect for request {}", authenticationRequest.getRelayState());
        if (logger.isTraceEnabled()) {
            logger.trace("redirect url: {}", redirectUrl);
        }

        response.sendRedirect(redirectUrl);
    }

    private void addParameter(String name, String value, UriComponentsBuilder builder) {
        Assert.hasText(name, "name cannot be empty or null");
        if (StringUtils.hasText(value)) {
            builder.queryParam(
                UriUtils.encode(name, StandardCharsets.ISO_8859_1),
                UriUtils.encode(value, StandardCharsets.ISO_8859_1)
            );
        }
    }

    private void sendPost(HttpServletResponse response, Saml2PostAuthenticationRequest authenticationRequest)
        throws IOException {
        String html = createSamlPostRequestFormData(authenticationRequest);
        logger.info("send post for request {}", authenticationRequest.getRelayState());
        if (logger.isTraceEnabled()) {
            logger.trace("post html: {}", html);
        }

        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.getWriter().write(html);
    }

    private AbstractSaml2AuthenticationRequest resolve(Saml2AuthenticationRequestContext context) {
        Saml2MessageBinding binding = context
            .getRelyingPartyRegistration()
            .getAssertingPartyDetails()
            .getSingleSignOnServiceBinding();

        if (binding == Saml2MessageBinding.REDIRECT) {
            return this.authenticationRequestFactory.createRedirectAuthenticationRequest(context);
        }
        return this.authenticationRequestFactory.createPostAuthenticationRequest(context);
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

    public void setAuthenticationRequestRepository(
        Saml2AuthenticationRequestRepository<SerializableSaml2AuthenticationRequestContext> authenticationRequestRepository
    ) {
        this.authenticationRequestRepository = authenticationRequestRepository;
    }

    private class CustomSpidAuthenticationRequestContextResolver implements Saml2AuthenticationRequestContextResolver {

        private final Converter<HttpServletRequest, RelyingPartyRegistration> relyingPartyRegistrationResolver;
        private final StringKeyGenerator stateGenerator = new Base64StringKeyGenerator(Base64.getUrlEncoder());

        public CustomSpidAuthenticationRequestContextResolver(
            Converter<HttpServletRequest, RelyingPartyRegistration> relyingPartyRegistrationResolver
        ) {
            this.relyingPartyRegistrationResolver = relyingPartyRegistrationResolver;
        }

        @Override
        public Saml2AuthenticationRequestContext resolve(HttpServletRequest request) {
            Assert.notNull(request, "request cannot be null");
            RelyingPartyRegistration relyingParty = this.relyingPartyRegistrationResolver.convert(request);
            if (relyingParty == null) {
                return null;
            }

            return createAuthenticationRequestContext(request, relyingParty);
        }

        private Saml2AuthenticationRequestContext createAuthenticationRequestContext(
            HttpServletRequest request,
            RelyingPartyRegistration relyingParty
        ) {
            return Saml2AuthenticationRequestContext
                .builder()
                .issuer(relyingParty.getEntityId())
                .relyingPartyRegistration(relyingParty)
                .assertionConsumerServiceUrl(relyingParty.getAssertionConsumerServiceLocation())
                .relayState(stateGenerator.generateKey())
                .build();
        }
    }
}
