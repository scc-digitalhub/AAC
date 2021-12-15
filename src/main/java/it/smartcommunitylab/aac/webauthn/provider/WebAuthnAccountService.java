package it.smartcommunitylab.aac.webauthn.provider;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.yubico.webauthn.data.ByteArray;

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
        List<WebAuthnUserAccount> accounts = userAccountService.findBySubject(getRealm(), subject);

        // we need to fix ids
        return accounts.stream().map(a -> {
            a.setProvider(getProvider());
            a.setUserId(exportInternalId(a.getUsername()));
            return a;
        }).collect(Collectors.toList());
    }

    @Override
    public WebAuthnUserAccount getAccount(String userId) throws NoSuchUserException {
        return userAccountService.getAccount(userId);
    }

    @Override
    public WebAuthnUserAccount getByIdentifyingAttributes(Map<String, String> attributes) throws NoSuchUserException {
        String realm = getRealm();

        // check if passed map contains at least one valid set and fetch account
        // TODO rewrite less hardcoded
        // note AVOID reflection, we want native image support
        WebAuthnUserAccount account = null;
        if (attributes.containsKey("userId")) {
            String username = parseResourceId(attributes.get("userId"));
            account = userAccountService.findByUsername(realm, username);
        }

        if (account == null
                && attributes.keySet().containsAll(Arrays.asList("realm", "username"))
                && realm.equals((attributes.get("realm")))) {
            account = userAccountService.findByUsername(realm, attributes.get("username"));
        }

        if (account == null
                && attributes.keySet().containsAll(Arrays.asList("realm", "email"))
                && realm.equals((attributes.get("realm")))) {
            account = userAccountService.findByEmail(realm, attributes.get("email"));
        }

        if (account == null
                && attributes.keySet().contains("credentialId")) {
            try {
                account = userAccountService.findByCredentialId(
                        ByteArray.fromBase64Url(attributes.get("credentialId")));
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
        account.setUserId(exportInternalId(username));

        return account;
    }

    @Override
    public void deleteAccount(String userId) throws NoSuchUserException {
        userAccountService.deleteAccount(userId);
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

        String realm = getRealm();

        // extract base fields
        String username = Jsoup.clean(reg.getUsername(), Safelist.none());

        // validate username
        if (!StringUtils.hasText(username)) {
            throw new RegistrationException("missing-username");
        }

        WebAuthnUserAccount account = userAccountService.findByUsername(realm, username);
        if (account != null) {
            throw new AlreadyRegisteredException("duplicate-registration");
        }

        // check type and extract our parameters if present
        WebAuthnCredential credential = null;
        String email = null;

        if (reg instanceof WebAuthnUserAccount) {
            WebAuthnUserAccount ireg = (WebAuthnUserAccount) reg;
            credential = ireg.getCredential();
            email = ireg.getEmailAddress();

            if (StringUtils.hasText(email)) {
                email = Jsoup.clean(email, Safelist.none());
            }

        }

        if (credential == null) {
            throw new IllegalArgumentException("Missing credential");
        }

        account = new WebAuthnUserAccount();
        account.setSubject(subjectId);
        account.setRealm(realm);
        account.setUsername(username);
        account.setCredential(credential);
        account.setEmailAddress(email);

        account = userAccountService.addAccount(account);

        String userId = this.exportInternalId(username);

        account.setUserId(userId);

        // set providerId since all webauthn accounts have the same
        account.setProvider(getProvider());

        // rewrite webauthn userId
        account.setUserId(exportInternalId(username));

        return account;

    }

    @Override
    public WebAuthnUserAccount updateAccount(String userId, UserAccount reg)
            throws NoSuchUserException, RegistrationException {
        if (!config.isEnableUpdate()) {
            throw new IllegalArgumentException("update is disabled for this provider");
        }

        String username = parseResourceId(userId);
        String realm = getRealm();

        WebAuthnUserAccount account = userAccountService.findByUsername(realm, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // can update only from our model
        if (reg instanceof WebAuthnUserAccount) {
            WebAuthnUserAccount ireg = (WebAuthnUserAccount) reg;
            String email = ireg.getEmailAddress();
            WebAuthnCredential credential = ireg.getCredential();

            if (StringUtils.hasText(email)) {
                email = Jsoup.clean(email, Safelist.none());
            }

            // we update all props, even if empty or null
            account.setEmailAddress(email);
            account.setCredential(credential);

            account = userAccountService.updateAccount(account.getId(), account);
        }

        // set providerId since all webauthn accounts have the same
        account.setProvider(getProvider());

        // rewrite webauthn userId
        account.setUserId(exportInternalId(username));

        return account;
    }
}
