package it.smartcommunitylab.aac.spid.provider;

import it.smartcommunitylab.aac.core.provider.FilterProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.spid.auth.SpidRelyingPartyRegistrationRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.util.Assert;

import javax.servlet.Filter;
import java.util.Collection;

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
        // TODO:
        return null;
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

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.eventPublisher = applicationEventPublisher;
    }
}
