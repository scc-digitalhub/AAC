package it.smartcommunitylab.aac.webauthn.provider;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.provider.AccountService;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredential;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccount;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnUserAccountService;

public class WebAuthnAccountService extends AbstractProvider implements AccountService {
    protected final WebAuthnUserAccountService userAccountService;

    private final WebAuthnIdentityProviderConfigMap config;

    public WebAuthnAccountService(String providerId,
            WebAuthnUserAccountService userAccountService,
            WebAuthnIdentityProviderConfig providerConfig, String realm) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, providerId, realm);
        Assert.notNull(providerConfig, "provider config is mandatory");
        Assert.notNull(userAccountService, "user account service is mandatory");
        this.userAccountService = userAccountService;
        this.config = providerConfig.getConfigMap();
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

    @Override
    public List<WebAuthnUserAccount> listAccounts(String subject) {
        List<WebAuthnUserAccount> accounts;
        try {
            accounts = userAccountService.findBySubjectAndRealm(subject, getRealm());
        } catch (NoSuchUserException e) {
            accounts = new LinkedList<>();
        }

        // we need to fix ids
        return accounts.stream().map(a -> {
            a.setProvider(getProvider());
            a.setSubject(exportInternalId(a.getUsername()));
            return a;
        }).collect(Collectors.toList());
    }

    @Override
    public WebAuthnUserAccount getAccount(String userId) throws NoSuchUserException {
        return userAccountService.findByProviderAndSubject(getProvider(), userId);
    }

    @Override
    public WebAuthnUserAccount getByIdentifyingAttributes(Map<String, String> attributes) throws NoSuchUserException {
        String provider = getProvider();

        // check if passed map contains at least one valid set and fetch account
        // TODO rewrite less hardcoded
        // note AVOID reflection, we want native image support
        WebAuthnUserAccount account = null;
        if (attributes.containsKey("userId")) {
            String subject = parseResourceId(attributes.get("userId"));
            account = userAccountService.findByProviderAndSubject(provider, subject);
        }

        if (account == null
                && attributes.keySet().containsAll(Arrays.asList("realm", "username"))
                && provider.equals((attributes.get("provider")))) {
            account = userAccountService
                    .findByProviderAndUsername(provider, attributes.get("username"));
        }

        if (account == null
                && attributes.keySet().contains("credentialId")) {
            try {
                account = userAccountService.findByCredentialId(
                        attributes.get("credentialId"));
            } catch (Exception e) {
            }
        }

        if (account == null) {
            throw new NoSuchUserException("No WebAuthn user found matching attributes");
        }

        String username = account.getUsername();

        // set providerId since all WebAuthn accounts have the same
        account.setProvider(getProvider());

        // rewrite webauthn userId
        account.setSubject(exportInternalId(username));

        return account;
    }

    @Override
    public void deleteAccount(String subject) throws NoSuchUserException {
        userAccountService.deleteAccount(getProvider(), subject);
    }

    @Override
    public boolean canRegister() {
        return config.isEnableRegistration();
    }

    @Override
    public boolean canUpdate() {
        return config.isEnableUpdate();
    }

    @Override
    public WebAuthnUserAccount registerAccount(String subjectId, UserAccount reg)
            throws NoSuchUserException, RegistrationException {
        if (!config.isEnableRegistration()) {
            throw new IllegalArgumentException("delete is disabled for this provider");
        }

        // we expect subject to be valid
        if (!StringUtils.hasText(subjectId)) {
            throw new RegistrationException("missing-subject");

        }

        String provider = getProvider();

        // extract base fields
        String username = Jsoup.clean(reg.getUsername(), Safelist.none());

        // validate username
        if (!StringUtils.hasText(username)) {
            throw new RegistrationException("missing-username");
        }

        WebAuthnUserAccount account = userAccountService
                .findByProviderAndUsername(provider, username);
        if (account != null) {
            throw new AlreadyRegisteredException("duplicate-registration");
        }

        // check type and extract our parameters if present
        Set<WebAuthnCredential> credentials = null;
        String email = null;

        if (reg instanceof WebAuthnUserAccount) {
            WebAuthnUserAccount ireg = (WebAuthnUserAccount) reg;
            credentials = ireg.getCredentials();
            email = ireg.getEmailAddress();

            if (StringUtils.hasText(email)) {
                email = Jsoup.clean(email, Safelist.none());
            }

        }

        if (credentials.isEmpty()) {
            throw new IllegalArgumentException("Missing credential");
        }

        account = new WebAuthnUserAccount();
        account.setSubject(subjectId);
        account.setRealm(getRealm());
        account.setUsername(username);
        account.setCredentials(credentials);
        account.setEmailAddress(email);

        account = userAccountService.addAccount(account);

        String userId = this.exportInternalId(username);

        account.setSubject(userId);

        // set providerId since all webauthn accounts have the same
        account.setProvider(getProvider());

        // rewrite webauthn userId
        account.setSubject(exportInternalId(username));

        return account;

    }

    @Override
    public WebAuthnUserAccount updateAccount(String userId, UserAccount reg)
            throws NoSuchUserException, RegistrationException {
        if (!config.isEnableUpdate()) {
            throw new IllegalArgumentException("update is disabled for this provider");
        }

        String username = parseResourceId(userId);
        String provider = getProvider();

        WebAuthnUserAccount account = userAccountService
                .findByProviderAndSubject(provider, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // can update only from our model
        if (reg instanceof WebAuthnUserAccount) {
            WebAuthnUserAccount ireg = (WebAuthnUserAccount) reg;
            String email = ireg.getEmailAddress();
            Set<WebAuthnCredential> credentials = ireg.getCredentials();

            if (StringUtils.hasText(email)) {
                email = Jsoup.clean(email, Safelist.none());
            }

            // we update all props, even if empty or null
            account.setEmailAddress(email);
            account.setCredentials(credentials);

            account = userAccountService.updateAccount(account.getId(), account);
        }

        // set providerId since all webauthn accounts have the same
        account.setProvider(getProvider());

        // rewrite webauthn userId
        account.setSubject(exportInternalId(username));

        return account;
    }

}
