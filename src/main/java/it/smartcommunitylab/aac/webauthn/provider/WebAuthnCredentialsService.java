package it.smartcommunitylab.aac.webauthn.provider;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.model.UserCredentials;
import it.smartcommunitylab.aac.core.provider.CredentialsService;
import it.smartcommunitylab.aac.webauthn.model.UserWebAuthnCredentials;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccount;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnUserAccountService;

public class WebAuthnCredentialsService extends AbstractProvider implements CredentialsService {

    private final WebAuthnIdentityProviderConfigMap config;
    private final WebAuthnUserAccountService userAccountService;

    public WebAuthnCredentialsService(String providerId, WebAuthnUserAccountService userAccountService,
            WebAuthnIdentityProviderConfig providerConfig,
            String realm) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, providerId, realm);
        Assert.notNull(userAccountService, "user account service is mandatory");
        Assert.notNull(providerConfig, "provider config is mandatory");
        this.userAccountService = userAccountService;
        this.config = providerConfig.getConfigMap();
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_CREDENTIALS;
    }

    @Override
    public boolean canRead() {
        return true;
    }

    @Override
    public boolean canSet() {
        return config.isEnableUpdate();
    }

    @Override
    public boolean canReset() {
        return config.isEnableReset();
    }

    @Override
    public UserWebAuthnCredentials getUserCredentials(String userId) throws NoSuchUserException {
        // fetch user
        String username = parseResourceId(userId);
        String provider = getProvider();
        WebAuthnUserAccount account = userAccountService
                .findByProviderAndUsername(provider, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        UserWebAuthnCredentials credentials = new UserWebAuthnCredentials();
        credentials.setUserId(userId);
        credentials.setCanReset(canReset());
        credentials.setCanSet(canSet());

        return credentials;
    }

    @Override
    public UserWebAuthnCredentials setUserCredentials(String userId, UserCredentials credentials)
            throws NoSuchUserException {
        if (!config.isEnableUpdate()) {
            throw new IllegalArgumentException("Updating the credential id is disabled for this provider");
        }

        // we support only webauthn credentials
        if (!(credentials instanceof UserWebAuthnCredentials)) {
            throw new IllegalArgumentException("invalid credentials");
        }

        String userHandle = ((UserWebAuthnCredentials) credentials).getCredentials();

        setCredential(userId, userHandle);

        // we return a placeholder to describe config
        UserWebAuthnCredentials result = new UserWebAuthnCredentials();
        result.setUserId(userId);
        result.setCanReset(canReset());
        result.setCanSet(canSet());

        return result;
    }

    public WebAuthnUserAccount setCredential(
            String userId,
            String userHandle) throws NoSuchUserException {

        // fetch user
        String username = parseResourceId(userId);
        String provider = getProvider();
        WebAuthnUserAccount account = userAccountService
                .findByProviderAndSubject(provider, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        return userAccountService.updateAccount(account.getId(), account);
    }

    @Override
    public UserWebAuthnCredentials resetUserCredentials(String userId) throws NoSuchUserException {
        throw new IllegalArgumentException("reset is disabled for this provider");
    }

    @Override
    public String getResetUrl() {
        return null;
    }

    @Override
    public String getSetUrl() throws NoSuchUserException {
        if (!config.isEnableUpdate()) {
            throw new IllegalArgumentException("Updating the credential id is disabled for this provider");
        }
        // internal controller route
        return "/changecred";
    }
}