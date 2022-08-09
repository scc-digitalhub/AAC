package it.smartcommunitylab.aac.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.Filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationRequestContext;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.CompositeFilter;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.ExtendedUserAuthenticationManager;
import it.smartcommunitylab.aac.core.auth.RequestAwareAuthenticationSuccessHandler;
import it.smartcommunitylab.aac.core.authorities.IdentityProviderAuthority;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwarePathUriBuilder;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.FilterProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.IdentityProviderAuthorityService;
import it.smartcommunitylab.aac.internal.InternalIdentityAuthority;
import it.smartcommunitylab.aac.internal.auth.InternalConfirmKeyAuthenticationFilter;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfig;
import it.smartcommunitylab.aac.internal.service.InternalUserAccountService;
import it.smartcommunitylab.aac.openid.apple.AppleIdentityAuthority;
import it.smartcommunitylab.aac.openid.apple.auth.AppleLoginAuthenticationFilter;
import it.smartcommunitylab.aac.openid.apple.provider.AppleIdentityProviderConfig;
import it.smartcommunitylab.aac.openid.auth.OIDCClientRegistrationRepository;
import it.smartcommunitylab.aac.openid.auth.OIDCLoginAuthenticationFilter;
import it.smartcommunitylab.aac.openid.auth.OIDCRedirectAuthenticationFilter;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProviderConfig;
import it.smartcommunitylab.aac.password.InternalPasswordIdentityAuthority;
import it.smartcommunitylab.aac.password.auth.InternalLoginAuthenticationFilter;
import it.smartcommunitylab.aac.password.auth.InternalResetKeyAuthenticationFilter;
import it.smartcommunitylab.aac.password.persistence.InternalUserPasswordRepository;
import it.smartcommunitylab.aac.password.provider.InternalPasswordIdentityProviderConfig;
import it.smartcommunitylab.aac.saml.auth.Saml2AuthenticationRequestRepository;
import it.smartcommunitylab.aac.saml.auth.SamlMetadataFilter;
import it.smartcommunitylab.aac.saml.auth.SamlRelyingPartyRegistrationRepository;
import it.smartcommunitylab.aac.saml.auth.SamlWebSsoAuthenticationFilter;
import it.smartcommunitylab.aac.saml.auth.SamlWebSsoAuthenticationRequestFilter;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProviderConfig;
import it.smartcommunitylab.aac.saml.service.HttpSessionSaml2AuthenticationRequestRepository;
import it.smartcommunitylab.aac.spid.auth.SpidMetadataFilter;
import it.smartcommunitylab.aac.spid.auth.SpidWebSsoAuthenticationFilter;
import it.smartcommunitylab.aac.spid.auth.SpidWebSsoAuthenticationRequestFilter;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfig;
import it.smartcommunitylab.aac.webauthn.auth.WebAuthnAuthenticationFilter;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfig;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnRpService;
import it.smartcommunitylab.aac.webauthn.store.WebAuthnAssertionRequestStore;

@Configuration
@Order(17)
public class AuthConfig {

    @Autowired
    private IdentityProviderAuthorityService identityProviderAuthorityService;

    @Autowired
    private ExtendedUserAuthenticationManager authManager;

    @Value("${application.url}")
    private String applicationURL;

    @Autowired
    @Qualifier("oidcClientRegistrationRepository")
    private OIDCClientRegistrationRepository oidcClientRegistrationRepository;
    @Autowired
    @Qualifier("appleClientRegistrationRepository")
    private OIDCClientRegistrationRepository appleClientRegistrationRepository;

    @Autowired
    @Qualifier("samlRelyingPartyRegistrationRepository")
    private SamlRelyingPartyRegistrationRepository samlRelyingPartyRegistrationRepository;

    @Autowired
    @Qualifier("spidRelyingPartyRegistrationRepository")
    private SamlRelyingPartyRegistrationRepository spidRelyingPartyRegistrationRepository;

    @Autowired
    private WebAuthnAssertionRequestStore webAuthnRequestStore;

    @Autowired
    private InternalUserAccountService internalUserAccountService;

    @Autowired
    private WebAuthnRpService webAuthnRpService;

    @Autowired
    private InternalUserPasswordRepository passwordRepository;

    @Autowired
    private RealmAwarePathUriBuilder realmUriBuilder;

    @Autowired
    private ProviderConfigRepository<InternalIdentityProviderConfig> internalProviderRepository;

    @Autowired
    private ProviderConfigRepository<InternalPasswordIdentityProviderConfig> internalPasswordProviderRepository;

    @Autowired
    private ProviderConfigRepository<WebAuthnIdentityProviderConfig> webAuthnProviderRepository;

    @Autowired
    private ProviderConfigRepository<OIDCIdentityProviderConfig> oidcProviderRepository;

    @Autowired
    private ProviderConfigRepository<SamlIdentityProviderConfig> samlProviderRepository;

    @Autowired
    private ProviderConfigRepository<SpidIdentityProviderConfig> spidProviderRepository;

    @Autowired
    private ProviderConfigRepository<AppleIdentityProviderConfig> appleProviderRepository;

    @Bean
    @Qualifier("authSecurityFilterChain")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.requestMatcher(getRequestMatcher())
                .authorizeRequests()
                .anyRequest().permitAll()
                .and()
                .authenticationManager(authManager)
                .addFilterBefore(
                        buildAuthoritiesFilters(),
                        BasicAuthenticationFilter.class)
                // we always want a session here
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.ALWAYS);

        return http.build();
    }

    private Filter buildAuthoritiesFilters() {
        List<Filter> filters = new ArrayList<>();

        // build filters for every authority
        Collection<IdentityProviderAuthority<UserIdentity, IdentityProvider<UserIdentity>>> authorities = identityProviderAuthorityService
                .getAuthorities();

        for (IdentityProviderAuthority<UserIdentity, IdentityProvider<UserIdentity>> authority : authorities) {
            // build filters for this authority via filterProvider from authority itself
            FilterProvider provider = authority.getFilterProvider();
            if (provider != null) {
                // we expect a list of filters
                Collection<Filter> pfs = provider.getFilters();
                if (filters != null) {
                    for (Filter filter : pfs) {
                        // register authManager for authFilters
                        if (filter instanceof AbstractAuthenticationProcessingFilter) {
                            ((AbstractAuthenticationProcessingFilter) filter).setAuthenticationManager(authManager);
                        }

                        filters.add(filter);
                    }
                }
            }
        }

        // build a virtual filter chain as composite filter
        CompositeFilter filter = new CompositeFilter();
        filter.setFilters(filters);

        return filter;
    }

    /*
     * Internal auth
     */
    private RequestAwareAuthenticationSuccessHandler successHandler() {
        return new RequestAwareAuthenticationSuccessHandler();
    }

    private CompositeFilter getInternalAuthorityFilters(AuthenticationManager authManager,
            ProviderConfigRepository<InternalIdentityProviderConfig> providerRepository,
            InternalUserAccountService userAccountService) {

        List<Filter> filters = new ArrayList<>();

        InternalConfirmKeyAuthenticationFilter<InternalIdentityProviderConfig> confirmKeyFilter = new InternalConfirmKeyAuthenticationFilter<>(
                userAccountService, providerRepository);
        confirmKeyFilter.setAuthenticationManager(authManager);
        confirmKeyFilter.setAuthenticationSuccessHandler(successHandler());

        filters.add(confirmKeyFilter);

        CompositeFilter filter = new CompositeFilter();
        filter.setFilters(filters);

        return filter;
    }

    private CompositeFilter getInternalPasswordAuthorityFilters(AuthenticationManager authManager,
            ProviderConfigRepository<InternalPasswordIdentityProviderConfig> providerRepository,
            InternalUserAccountService userAccountService, InternalUserPasswordRepository passwordRepository) {

        List<Filter> filters = new ArrayList<>();

        InternalLoginAuthenticationFilter loginFilter = new InternalLoginAuthenticationFilter(
                userAccountService, passwordRepository, providerRepository);
        loginFilter.setAuthenticationManager(authManager);
        loginFilter.setAuthenticationSuccessHandler(successHandler());
        filters.add(loginFilter);

        InternalConfirmKeyAuthenticationFilter<InternalPasswordIdentityProviderConfig> confirmKeyFilter = new InternalConfirmKeyAuthenticationFilter<>(
                SystemKeys.AUTHORITY_PASSWORD,
                userAccountService, providerRepository,
                InternalPasswordIdentityAuthority.AUTHORITY_URL + "confirm/{registrationId}", null);
        confirmKeyFilter.setAuthenticationManager(authManager);
        confirmKeyFilter.setAuthenticationSuccessHandler(successHandler());

        filters.add(confirmKeyFilter);

        InternalResetKeyAuthenticationFilter resetKeyFilter = new InternalResetKeyAuthenticationFilter(
                userAccountService, passwordRepository, providerRepository);
        resetKeyFilter.setAuthenticationManager(authManager);
        resetKeyFilter.setAuthenticationSuccessHandler(successHandler());
        filters.add(resetKeyFilter);

        CompositeFilter filter = new CompositeFilter();
        filter.setFilters(filters);

        return filter;
    }

    private CompositeFilter getWebAuthnAuthorityFilters(AuthenticationManager authManager,
            WebAuthnRpService rpService,
            ProviderConfigRepository<WebAuthnIdentityProviderConfig> providerRepository,
            InternalUserAccountService userAccountService, WebAuthnAssertionRequestStore requestStore) {

        List<Filter> filters = new ArrayList<>();

        WebAuthnAuthenticationFilter loginFilter = new WebAuthnAuthenticationFilter(rpService, requestStore,
                providerRepository);
        loginFilter.setAuthenticationManager(authManager);
        loginFilter.setAuthenticationSuccessHandler(successHandler());
        filters.add(loginFilter);

        CompositeFilter filter = new CompositeFilter();
        filter.setFilters(filters);

        return filter;
    }

    /*
     * OIDC Auth
     */

    private CompositeFilter getOIDCAuthorityFilters(AuthenticationManager authManager,
            ProviderConfigRepository<OIDCIdentityProviderConfig> providerRepository,
            OIDCClientRegistrationRepository clientRegistrationRepository) {
        // build filters bound to shared client + request repos
        AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository = new HttpSessionOAuth2AuthorizationRequestRepository();
        OIDCRedirectAuthenticationFilter redirectFilter = new OIDCRedirectAuthenticationFilter(
                providerRepository,
                clientRegistrationRepository);
        redirectFilter.setAuthorizationRequestRepository(authorizationRequestRepository);

        OIDCLoginAuthenticationFilter loginFilter = new OIDCLoginAuthenticationFilter(
                providerRepository,
                clientRegistrationRepository);
        loginFilter.setAuthorizationRequestRepository(authorizationRequestRepository);
        loginFilter.setAuthenticationManager(authManager);

        List<Filter> filters = new ArrayList<>();
        filters.add(loginFilter);
        filters.add(redirectFilter);

        CompositeFilter filter = new CompositeFilter();
        filter.setFilters(filters);

        return filter;
    }

    /*
     * Saml2 Auth
     */

    private CompositeFilter getSamlAuthorityFilters(AuthenticationManager authManager,
            ProviderConfigRepository<SamlIdentityProviderConfig> providerRepository,
            SamlRelyingPartyRegistrationRepository relyingPartyRegistrationRepository) {

        // request repository
        Saml2AuthenticationRequestRepository<Saml2AuthenticationRequestContext> authenticationRequestRepository = new HttpSessionSaml2AuthenticationRequestRepository();

        // build filters
        SamlWebSsoAuthenticationRequestFilter requestFilter = new SamlWebSsoAuthenticationRequestFilter(
                providerRepository,
                relyingPartyRegistrationRepository);
        requestFilter.setAuthenticationRequestRepository(authenticationRequestRepository);

        SamlWebSsoAuthenticationFilter ssoFilter = new SamlWebSsoAuthenticationFilter(
                providerRepository,
                relyingPartyRegistrationRepository);
        ssoFilter.setAuthenticationManager(authManager);
        ssoFilter.setAuthenticationRequestRepository(authenticationRequestRepository);

        SamlMetadataFilter metadataFilter = new SamlMetadataFilter(relyingPartyRegistrationRepository);

        List<Filter> filters = new ArrayList<>();
        filters.add(metadataFilter);
        filters.add(requestFilter);
        filters.add(ssoFilter);

        CompositeFilter filter = new CompositeFilter();
        filter.setFilters(filters);

        return filter;
    }

    /*
     * SPID Auth
     */

    private CompositeFilter getSpidAuthorityFilters(AuthenticationManager authManager,
            ProviderConfigRepository<SpidIdentityProviderConfig> providerRepository,
            SamlRelyingPartyRegistrationRepository relyingPartyRegistrationRepository) {

        // request repository
        Saml2AuthenticationRequestRepository<Saml2AuthenticationRequestContext> authenticationRequestRepository = new HttpSessionSaml2AuthenticationRequestRepository();

        // build filters
        SpidWebSsoAuthenticationRequestFilter requestFilter = new SpidWebSsoAuthenticationRequestFilter(
                providerRepository,
                relyingPartyRegistrationRepository);
        requestFilter.setAuthenticationRequestRepository(authenticationRequestRepository);

        SpidWebSsoAuthenticationFilter ssoFilter = new SpidWebSsoAuthenticationFilter(
                providerRepository,
                relyingPartyRegistrationRepository);
        ssoFilter.setAuthenticationManager(authManager);
        ssoFilter.setAuthenticationRequestRepository(authenticationRequestRepository);

        SpidMetadataFilter metadataFilter = new SpidMetadataFilter(providerRepository,
                relyingPartyRegistrationRepository);

        List<Filter> filters = new ArrayList<>();
        filters.add(metadataFilter);
        filters.add(requestFilter);
        filters.add(ssoFilter);

        CompositeFilter filter = new CompositeFilter();
        filter.setFilters(filters);

        return filter;
    }

    /*
     * Apple Auth
     */

    private CompositeFilter getAppleAuthorityFilters(AuthenticationManager authManager,
            ProviderConfigRepository<AppleIdentityProviderConfig> providerRepository,
            OIDCClientRegistrationRepository clientRegistrationRepository) {
        // build filters bound to shared client + request repos
        AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository = new HttpSessionOAuth2AuthorizationRequestRepository();

        OAuth2AuthorizationRequestRedirectFilter redirectFilter = new OAuth2AuthorizationRequestRedirectFilter(
                clientRegistrationRepository, AppleIdentityAuthority.AUTHORITY_URL + "authorize");
        redirectFilter.setAuthorizationRequestRepository(authorizationRequestRepository);

        AppleLoginAuthenticationFilter loginFilter = new AppleLoginAuthenticationFilter(
                providerRepository,
                clientRegistrationRepository);
        loginFilter.setAuthorizationRequestRepository(authorizationRequestRepository);
        loginFilter.setAuthenticationManager(authManager);

        List<Filter> filters = new ArrayList<>();
        filters.add(loginFilter);
        filters.add(redirectFilter);

        CompositeFilter filter = new CompositeFilter();
        filter.setFilters(filters);

        return filter;
    }

    private RequestMatcher getRequestMatcher() {
        return new AntPathRequestMatcher(AUTH_URL);
    }

    private static final String AUTH_URL = "/auth/**";

}
