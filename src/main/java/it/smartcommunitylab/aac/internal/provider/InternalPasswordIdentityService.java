package it.smartcommunitylab.aac.internal.provider;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.internal.persistence.InternalUserPassword;
import it.smartcommunitylab.aac.internal.persistence.InternalUserPasswordRepository;
import it.smartcommunitylab.aac.internal.service.InternalUserAccountService;
import it.smartcommunitylab.aac.utils.MailService;

public class InternalPasswordIdentityService extends InternalIdentityService<InternalUserPassword> {

    private final InternalPasswordService passwordService;
    private final InternalAuthenticationProvider authenticationProvider;

    public InternalPasswordIdentityService(
            String providerId,
            InternalUserAccountService userAccountService,
            UserEntityService userEntityService, SubjectService subjectService,
            InternalUserPasswordRepository passwordRepository,
            InternalIdentityProviderConfig config,
            String realm) {
        super(providerId,
                userAccountService,
                userEntityService, subjectService,
                config, realm);
        Assert.notNull(passwordRepository, "password repository is mandatory");

        // build providers
        this.passwordService = new InternalPasswordService(providerId, userAccountService, passwordRepository, config,
                realm);
        this.authenticationProvider = new InternalAuthenticationProvider(providerId, userAccountService, accountService,
                passwordService, config, realm);
    }

    @Override
    public void setMailService(MailService mailService) {
        super.setMailService(mailService);
        this.passwordService.setMailService(mailService);
    }

    @Override
    public void setUriBuilder(RealmAwareUriBuilder uriBuilder) {
        super.setUriBuilder(uriBuilder);
        this.passwordService.setUriBuilder(uriBuilder);
    }

    @Override
    public InternalAuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    @Override
    public InternalPasswordService getCredentialsService() {
        return passwordService;
    }

    @Override
    @Transactional(readOnly = false)
    public void deleteIdentity(String username) throws NoSuchUserException {
        // remove all credentials
        passwordService.deleteCredentials(username);

        // call super to remove account
        super.deleteIdentity(username);
    }

    @Override
    public String getLoginForm() {
        return "password_form";
    }
}
