package it.smartcommunitylab.aac.spid.provider;

import it.smartcommunitylab.aac.core.provider.FilterProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.saml.auth.Saml2AuthenticationRequestRepository;
import it.smartcommunitylab.aac.saml.auth.SamlWebSsoAuthenticationRequestFilter;
import it.smartcommunitylab.aac.saml.auth.SerializableSaml2AuthenticationRequestContext;
import it.smartcommunitylab.aac.saml.service.HttpSessionSaml2AuthenticationRequestRepository;
import it.smartcommunitylab.aac.spid.auth.SpidRelyingPartyRegistrationRepository;
import it.smartcommunitylab.aac.spid.auth.SpidWebSsoAuthenticationRequestFilter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.util.Assert;

import javax.servlet.Filter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SpidFilterProvider implements FilterProvider, ApplicationEventPublisherAware {
    private String authorityId;
    private final ProviderConfigRepository<SpidIdentityProviderConfig> providerConfigRepository;
    private final SpidRelyingPartyRegistrationRepository relyingPartyRegistrationRepository;

    private ApplicationEventPublisher eventPublisher;

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
        Saml2AuthenticationRequestRepository<SerializableSaml2AuthenticationRequestContext> authenticationRequestRepository =
                new HttpSessionSaml2AuthenticationRequestRepository();

        // build filters
        SpidWebSsoAuthenticationRequestFilter requestFilter = new SpidWebSsoAuthenticationRequestFilter(
                authorityId,
                providerConfigRepository,
                relyingPartyRegistrationRepository,
                buildFilterUrl("authenticate/{registrationId}")
        );
        return filters;
    }

    @Override
    public Collection<Filter> getChainFilters() {
        // TODO:
        return null;
    }

    @Override
    public Collection<String> getCorsIgnoringAntMatchers() {
        // TODO:
        return null;
    }

    private String buildFilterUrl(String action) {
        // always use same path building logic for saml
        return "/auth/" + authorityId + "/" + action;
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.eventPublisher = applicationEventPublisher;
    }
}
