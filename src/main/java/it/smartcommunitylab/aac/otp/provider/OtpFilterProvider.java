package it.smartcommunitylab.aac.otp.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.core.auth.RequestAwareAuthenticationSuccessHandler;
import it.smartcommunitylab.aac.core.provider.FilterProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.internal.model.InternalUserAccount;
import it.smartcommunitylab.aac.otp.auth.UsernameOtpAuthenticationFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.servlet.Filter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.util.Assert;

public class OtpFilterProvider implements FilterProvider {

    private final ProviderConfigRepository<OtpIdentityProviderConfig> registrationRepository;
    private final UserAccountService<InternalUserAccount> userAccountService;

    private AuthenticationManager authManager;

    public OtpFilterProvider(
        UserAccountService<InternalUserAccount> userAccountService,
        ProviderConfigRepository<OtpIdentityProviderConfig> registrationRepository
    ) {
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
        return SystemKeys.AUTHORITY_OTP;
    }

    @Override
    public List<Filter> getAuthFilters() {
        UsernameOtpAuthenticationFilter loginFilter = new UsernameOtpAuthenticationFilter(
            userAccountService,
            registrationRepository
        );
        loginFilter.setAuthenticationSuccessHandler(new RequestAwareAuthenticationSuccessHandler());

        if (authManager != null) {
            loginFilter.setAuthenticationManager(authManager);
        }

        List<Filter> filters = new ArrayList<>();
        filters.add(loginFilter);
        return filters;
    }

    @Override
    public Collection<Filter> getChainFilters() {
        return null;
    }

    @Override
    public Collection<String> getCorsIgnoringAntMatchers() {
        return List.of();
    }
}
