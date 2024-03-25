package it.smartcommunitylab.aac.spid.provider;

import it.smartcommunitylab.aac.core.provider.FilterProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.saml.auth.Saml2AuthenticationRequestRepository;
import it.smartcommunitylab.aac.saml.auth.SamlWebSsoAuthenticationRequestFilter;
import it.smartcommunitylab.aac.saml.auth.SerializableSaml2AuthenticationRequestContext;
import it.smartcommunitylab.aac.saml.service.HttpSessionSaml2AuthenticationRequestRepository;
import it.smartcommunitylab.aac.spid.auth.SpidMetadataFilter;
import it.smartcommunitylab.aac.spid.auth.SpidRelyingPartyRegistrationRepository;
import it.smartcommunitylab.aac.spid.auth.SpidWebSsoAuthenticationFilter;
import it.smartcommunitylab.aac.spid.auth.SpidWebSsoAuthenticationRequestFilter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.util.Assert;

import javax.servlet.Filter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/*
 * SpidFilterProvider generates 3 filters:
 *  a. SpidWebSsoAuthenticationRequestFilter, which initiates an authentication request
 *  b. SpidWebSsoAuthenticationFilter, which collects authentication responses
 *  c. SpidMetadataFilter, which displays the metadata of the service as SAML SP
 * For more oh what those filters are, see this reference page on Spring Security Architecture
 *  https://docs.spring.io/spring-security/reference/servlet/saml2/login/overview.html
 */
public class SpidFilterProvider implements FilterProvider, ApplicationEventPublisherAware {
    private static final String[] NO_CORS_ENDPOINTS = { "authenticate/**", "sso/**" }; // must match filters endpoints
    private final String authorityId;
    private final ProviderConfigRepository<SpidIdentityProviderConfig> providerConfigRepository;
    private final SpidRelyingPartyRegistrationRepository relyingPartyRegistrationRepository;

    private ApplicationEventPublisher eventPublisher;
    private AuthenticationManager authManager;

    public SpidFilterProvider(
        String authorityId,
        SpidRelyingPartyRegistrationRepository relyingPartyRegistrationRepository,
        ProviderConfigRepository<SpidIdentityProviderConfig> providerConfigRepository
    ) {
        Assert.hasText(authorityId, "authority can not be null or empty");
        Assert.notNull(providerConfigRepository, "registration repository is mandatory");
        Assert.notNull(relyingPartyRegistrationRepository, "relying party registration repository is mandatory");

        this.authorityId = authorityId;
        this.providerConfigRepository = providerConfigRepository;
        this.relyingPartyRegistrationRepository = relyingPartyRegistrationRepository;
    }

    @Override
    public String getAuthorityId() {
        return authorityId;
    }

    @Override
    public Collection<Filter> getAuthFilters() {
        List<Filter> filters = new ArrayList<>();
        // build request repository bound to session
        Saml2AuthenticationRequestRepository<SerializableSaml2AuthenticationRequestContext> authenticationRequestRepository =
                new HttpSessionSaml2AuthenticationRequestRepository();

        // build filters
        SpidWebSsoAuthenticationRequestFilter requestFilter = new SpidWebSsoAuthenticationRequestFilter(
            authorityId,
            providerConfigRepository,
            relyingPartyRegistrationRepository,
            buildFilterUrl("authenticate/{registrationId}")
        );
        requestFilter.setApplicationEventPublisher(eventPublisher);
        requestFilter.setAuthenticationRequestRepository(authenticationRequestRepository);

        SpidWebSsoAuthenticationFilter ssoFilter = new SpidWebSsoAuthenticationFilter(
            providerConfigRepository,
            relyingPartyRegistrationRepository,
            buildFilterUrl("sso/{registrationId}"),
            null
        );
        ssoFilter.setAuthenticationRequestRepository(authenticationRequestRepository);
        if (this.authManager != null) {
            ssoFilter.setAuthenticationManager(authManager);
        }

        SpidMetadataFilter metadataFilter = new SpidMetadataFilter(
            providerConfigRepository,
            relyingPartyRegistrationRepository
        );

        filters.add(requestFilter);
        filters.add(ssoFilter);
        filters.add(metadataFilter);

        return filters;
    }

    @Override
    public Collection<Filter> getChainFilters() {
        return null;
    }

    @Override
    public Collection<String> getCorsIgnoringAntMatchers() {
        return Arrays
            .stream(NO_CORS_ENDPOINTS)
            .map(a -> "/auth/" + authorityId + "/" + a)
            .collect(Collectors.toList());
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.eventPublisher = applicationEventPublisher;
    }

    public void setAuthManager(AuthenticationManager authManager) {
        this.authManager = authManager;
    }

    private String buildFilterUrl(String action) {
        // always use same path building logic for saml
        return "/auth/" + authorityId + "/" + action;
    }
}
