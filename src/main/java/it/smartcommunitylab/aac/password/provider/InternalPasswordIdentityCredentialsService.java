package it.smartcommunitylab.aac.password.provider;

import java.util.HashMap;
import java.util.Map;

import javax.mail.MessagingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.password.InternalPasswordIdentityAuthority;
import it.smartcommunitylab.aac.password.persistence.InternalUserPassword;
import it.smartcommunitylab.aac.password.service.InternalUserPasswordService;
import it.smartcommunitylab.aac.utils.MailService;

@Transactional
public class InternalPasswordIdentityCredentialsService extends AbstractProvider<InternalUserPassword> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final InternalPasswordIdentityProviderConfig config;

    private final InternalUserPasswordService passwordService;
    private final UserAccountService<InternalUserAccount> accountService;
    private final String repositoryId;

    private MailService mailService;
    private RealmAwareUriBuilder uriBuilder;

    public InternalPasswordIdentityCredentialsService(String providerId,
            UserAccountService<InternalUserAccount> accountService, InternalUserPasswordService passwordService,
            InternalPasswordIdentityProviderConfig config, String realm) {
        super(SystemKeys.AUTHORITY_PASSWORD, providerId, realm);
        Assert.notNull(accountService, "account service is mandatory");
        Assert.notNull(passwordService, "password service is mandatory");
        Assert.notNull(config, "config is mandatory");

        this.config = config;

        this.accountService = accountService;
        this.passwordService = passwordService;

        // repositoryId from config
        this.repositoryId = config.getRepositoryId();
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_CREDENTIALS;
    }

    public void setMailService(MailService mailService) {
        this.mailService = mailService;
    }

    public void setUriBuilder(RealmAwareUriBuilder uriBuilder) {
        this.uriBuilder = uriBuilder;
    }

    @Transactional(readOnly = true)
    public InternalUserPassword findPassword(String username) throws NoSuchUserException {
        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        return passwordService.findPassword(repositoryId, username);
    }

    public boolean verifyPassword(String username, String password) throws NoSuchUserException {
        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        return passwordService.verifyPassword(repositoryId, username, password);
    }

    public void resetPassword(String username) throws NoSuchUserException {
        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // reset credentials by generating the key
        InternalUserPassword pass = passwordService.resetPassword(repositoryId, username,
                config.getPasswordResetValidity());

        // send mail
        try {
            sendResetMail(account, pass.getResetKey());
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public boolean confirmReset(String resetKey) throws NoSuchCredentialException {
        InternalUserPassword pass = passwordService.confirmReset(repositoryId, resetKey);
        return pass != null;
    }

    public void deletePassword(String username) throws NoSuchUserException {
        passwordService.deletePassword(repositoryId, username);
    }

    private void sendResetMail(InternalUserAccount account, String key) throws MessagingException {
        if (mailService != null) {
            // action is handled by global filter
            String provider = getProvider();
            String resetUrl = InternalPasswordIdentityAuthority.AUTHORITY_URL + "doreset/" + provider + "?code=" + key;
            if (uriBuilder != null) {
                resetUrl = uriBuilder.buildUrl(null, resetUrl);
            }

            Map<String, String> action = new HashMap<>();
            action.put("url", resetUrl);
            action.put("text", "action.reset");

            Map<String, Object> vars = new HashMap<>();
            vars.put("user", account);
            vars.put("action", action);
            vars.put("realm", account.getRealm());

            String template = "reset";
            mailService.sendEmail(account.getEmail(), template, account.getLang(), vars);
        }
    }
}
