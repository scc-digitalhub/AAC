package it.smartcommunitylab.aac.password.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.servlet.Filter;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.util.Assert;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.RequestAwareAuthenticationSuccessHandler;
import it.smartcommunitylab.aac.core.provider.FilterProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.internal.auth.InternalConfirmKeyAuthenticationFilter;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.service.InternalUserConfirmKeyService;
import it.smartcommunitylab.aac.password.InternalPasswordIdentityAuthority;
import it.smartcommunitylab.aac.password.auth.InternalLoginAuthenticationFilter;
import it.smartcommunitylab.aac.password.auth.InternalResetKeyAuthenticationFilter;
import it.smartcommunitylab.aac.password.persistence.InternalUserPasswordRepository;

public class InternalPasswordFilterProvider implements FilterProvider {

    private final ProviderConfigRepository<InternalPasswordIdentityProviderConfig> registrationRepository;
    private final UserAccountService<InternalUserAccount> userAccountService;
    // TODO replace with credentials service when available as independent service
    private final InternalUserPasswordRepository passwordRepository;

    // TODO remove
    private final InternalUserConfirmKeyService confirmKeyService;

    private AuthenticationManager authManager;

    public InternalPasswordFilterProvider(
            UserAccountService<InternalUserAccount> userAccountService, InternalUserConfirmKeyService confirmKeyService,
            InternalUserPasswordRepository passwordRepository,
            ProviderConfigRepository<InternalPasswordIdentityProviderConfig> registrationRepository) {
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(confirmKeyService, "confirm key service is mandatory");
        Assert.notNull(passwordRepository, "password repository is mandatory");
        Assert.notNull(registrationRepository, "registration repository is mandatory");

        this.userAccountService = userAccountService;
        this.confirmKeyService = confirmKeyService;
        this.passwordRepository = passwordRepository;
        this.registrationRepository = registrationRepository;
    }

    public void setAuthManager(AuthenticationManager authManager) {
        this.authManager = authManager;
    }

    @Override
    public String getAuthorityId() {
        return SystemKeys.AUTHORITY_PASSWORD;
    }

    @Override
    public List<Filter> getFilters() {

        InternalLoginAuthenticationFilter loginFilter = new InternalLoginAuthenticationFilter(
                userAccountService, passwordRepository, registrationRepository);
        loginFilter.setAuthenticationSuccessHandler(successHandler());

        InternalResetKeyAuthenticationFilter resetKeyFilter = new InternalResetKeyAuthenticationFilter(
                userAccountService, passwordRepository, registrationRepository);
        resetKeyFilter.setAuthenticationSuccessHandler(successHandler());

        // TODO remove when registration is handled only by internalService
        InternalConfirmKeyAuthenticationFilter<InternalPasswordIdentityProviderConfig> confirmKeyFilter = new InternalConfirmKeyAuthenticationFilter<>(
                SystemKeys.AUTHORITY_PASSWORD,
                confirmKeyService, registrationRepository,
                InternalPasswordIdentityAuthority.AUTHORITY_URL + "confirm/{registrationId}", null);
        confirmKeyFilter.setAuthenticationSuccessHandler(successHandler());

        if (authManager != null) {
            loginFilter.setAuthenticationManager(authManager);
            confirmKeyFilter.setAuthenticationManager(authManager);
            resetKeyFilter.setAuthenticationManager(authManager);
        }

        // build composite filterChain
        List<Filter> filters = new ArrayList<>();
        filters.add(loginFilter);
        filters.add(resetKeyFilter);
        filters.add(confirmKeyFilter);

        return filters;

    }

    @Override
    public Collection<String> getCorsIgnoringAntMatchers() {
        return Arrays.asList(NO_CORS_ENDPOINTS);
    }

    private static String[] NO_CORS_ENDPOINTS = {
    };

    private RequestAwareAuthenticationSuccessHandler successHandler() {
        return new RequestAwareAuthenticationSuccessHandler();
    }
}
