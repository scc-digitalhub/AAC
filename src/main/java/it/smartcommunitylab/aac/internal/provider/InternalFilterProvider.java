package it.smartcommunitylab.aac.internal.provider;

import java.util.Collection;
import java.util.Collections;

import javax.servlet.Filter;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.util.Assert;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.RequestAwareAuthenticationSuccessHandler;
import it.smartcommunitylab.aac.core.provider.FilterProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.internal.auth.InternalConfirmKeyAuthenticationFilter;
import it.smartcommunitylab.aac.internal.service.InternalUserAccountService;

public class InternalFilterProvider implements FilterProvider {

    private final ProviderConfigRepository<InternalIdentityProviderConfig> registrationRepository;
    private final InternalUserAccountService userAccountService;
    private AuthenticationManager authManager;

    public InternalFilterProvider(InternalUserAccountService userAccountService,
            ProviderConfigRepository<InternalIdentityProviderConfig> registrationRepository) {
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(registrationRepository, "registration repository is mandatory");

        this.userAccountService = userAccountService;
        this.registrationRepository = registrationRepository;
    }

    public void setAuthManager(AuthenticationManager authManager) {
        this.authManager = authManager;
    }

    @Override
    public String getAuthorityId() {
        return SystemKeys.AUTHORITY_INTERNAL;
    }

    @Override
    public Collection<Filter> getFilters() {

        // we expose only the confirmKey auth filter with default config
        InternalConfirmKeyAuthenticationFilter<InternalIdentityProviderConfig> confirmKeyFilter = new InternalConfirmKeyAuthenticationFilter<>(
                userAccountService, registrationRepository);
        confirmKeyFilter.setAuthenticationSuccessHandler(successHandler());

        if (authManager != null) {
            confirmKeyFilter.setAuthenticationManager(authManager);
        }

        return Collections.singletonList(confirmKeyFilter);
    }

    private RequestAwareAuthenticationSuccessHandler successHandler() {
        return new RequestAwareAuthenticationSuccessHandler();
    }
}
