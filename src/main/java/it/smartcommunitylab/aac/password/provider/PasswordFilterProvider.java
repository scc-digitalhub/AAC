package it.smartcommunitylab.aac.password.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.RequestAwareAuthenticationSuccessHandler;
import it.smartcommunitylab.aac.core.provider.FilterProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.password.auth.ResetKeyAuthenticationFilter;
import it.smartcommunitylab.aac.password.auth.UsernamePasswordAuthenticationFilter;
import it.smartcommunitylab.aac.password.service.InternalPasswordUserCredentialsService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.servlet.Filter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.util.Assert;

public class PasswordFilterProvider implements FilterProvider {

    private final ProviderConfigRepository<PasswordIdentityProviderConfig> registrationRepository;
    private final UserAccountService<InternalUserAccount> userAccountService;
    private final InternalPasswordUserCredentialsService userPasswordService;

    private AuthenticationManager authManager;

    public PasswordFilterProvider(
        UserAccountService<InternalUserAccount> userAccountService,
        InternalPasswordUserCredentialsService userPasswordService,
        ProviderConfigRepository<PasswordIdentityProviderConfig> registrationRepository
    ) {
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(userPasswordService, "password service is mandatory");
        Assert.notNull(registrationRepository, "registration repository is mandatory");

        this.userAccountService = userAccountService;
        this.userPasswordService = userPasswordService;
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
    public List<Filter> getAuthFilters() {
        // build auth filters for user+password and resetKey
        UsernamePasswordAuthenticationFilter loginFilter = new UsernamePasswordAuthenticationFilter(
            userAccountService,
            userPasswordService,
            registrationRepository
        );
        loginFilter.setAuthenticationSuccessHandler(successHandler());

        ResetKeyAuthenticationFilter resetKeyFilter = new ResetKeyAuthenticationFilter(
            userAccountService,
            userPasswordService,
            registrationRepository
        );
        resetKeyFilter.setAuthenticationSuccessHandler(successHandler());

        if (authManager != null) {
            loginFilter.setAuthenticationManager(authManager);
            resetKeyFilter.setAuthenticationManager(authManager);
        }

        // build composite filterChain
        List<Filter> filters = new ArrayList<>();
        filters.add(loginFilter);
        filters.add(resetKeyFilter);

        return filters;
    }

    @Override
    public Collection<Filter> getChainFilters() {
        // TODO build chain filter to check password set/expire/reset etc
        return null;
    }

    @Override
    public Collection<String> getCorsIgnoringAntMatchers() {
        return Arrays.asList(NO_CORS_ENDPOINTS);
    }

    private static String[] NO_CORS_ENDPOINTS = {};

    private RequestAwareAuthenticationSuccessHandler successHandler() {
        return new RequestAwareAuthenticationSuccessHandler();
    }
}
