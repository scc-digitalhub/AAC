package it.smartcommunitylab.aac.webauthn.provider;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.common.InvalidDataException;
import it.smartcommunitylab.aac.common.MissingDataException;
import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.base.AbstractProviderConfig;
import it.smartcommunitylab.aac.core.model.UserCredentials;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.provider.UserCredentialsService;
import it.smartcommunitylab.aac.internal.model.CredentialsStatus;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserCredential;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserCredentialsRepository;

@Transactional
public class WebAuthnCredentialsService extends AbstractProvider<WebAuthnUserCredential>
        implements UserCredentialsService<WebAuthnUserCredential> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final UserAccountService<InternalUserAccount> accountService;

    // TODO replace with service to detach from JPA
    private WebAuthnUserCredentialsRepository credentialsRepository;

    // provider configuration
    private final WebAuthnIdentityProviderConfig config;
    private final String repositoryId;

    public WebAuthnCredentialsService(String providerId, UserAccountService<InternalUserAccount> userAccountService,
            WebAuthnUserCredentialsRepository credentialsRepository,
            WebAuthnIdentityProviderConfig providerConfig,
            String realm) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, providerId, realm);
        Assert.notNull(userAccountService, "user account service is mandatory");
        Assert.notNull(credentialsRepository, "credentials repository is mandatory");
        Assert.notNull(providerConfig, "provider config is mandatory");
        this.accountService = userAccountService;
        this.credentialsRepository = credentialsRepository;
        this.config = providerConfig;
        this.repositoryId = config.getRepositoryId();
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_CREDENTIALS;
    }

    @Transactional(readOnly = true)
    public WebAuthnUserCredential findCredentialById(String id) {
        WebAuthnUserCredential c = credentialsRepository.findOne(id);
        if (c == null) {
            return null;
        }

        if (!c.getProvider().equals(repositoryId)) {
            return null;
        }

        return credentialsRepository.detach(c);
    }

    /*
     * Credentials handling
     */

    @Transactional(readOnly = true)
    public List<WebAuthnUserCredential> findCredentialsByUsername(String username) {
        List<WebAuthnUserCredential> credentials = credentialsRepository.findByProviderAndUsername(repositoryId, username);
        return credentials.stream()
                .map(a -> {
                    return credentialsRepository.detach(a);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WebAuthnUserCredential> findActiveCredentialsByUsername(String username) {
        List<WebAuthnUserCredential> credentials = credentialsRepository.findByProviderAndUsername(repositoryId, username);
        return credentials.stream()
                .filter(c -> STATUS_ACTIVE.equals(c.getStatus()))
                .map(a -> {
                    return credentialsRepository.detach(a);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WebAuthnUserCredential> findActiveCredentialsByUserHandle(String userHandle) {
        List<WebAuthnUserCredential> credentials = credentialsRepository.findByProviderAndUserHandle(repositoryId,
                userHandle);
        return credentials.stream()
                .filter(c -> STATUS_ACTIVE.equals(c.getStatus()))
                .map(a -> {
                    return credentialsRepository.detach(a);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WebAuthnUserCredential> findCredentialsByCredentialId(String credentialId) {
        List<WebAuthnUserCredential> credentials = credentialsRepository.findByProviderAndCredentialId(repositoryId,
                credentialId);
        return credentials.stream()
                .map(a -> {
                    return credentialsRepository.detach(a);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WebAuthnUserCredential findCredentialByUserHandleAndCredentialId(String userHandle,
            String credentialId) {
        WebAuthnUserCredential c = credentialsRepository.findByProviderAndUserHandleAndCredentialId(repositoryId,
                userHandle,
                credentialId);
        if (c == null) {
            return null;
        }

        return credentialsRepository.detach(c);
    }

    public WebAuthnUserCredential addCredential(String userHandle, String credentialId,
            WebAuthnUserCredential reg)
            throws RegistrationException {

        // extract relevant data
        String username = reg.getUsername();
        if (!StringUtils.hasText(username)) {
            throw new MissingDataException("username");
        }

        String publicKeyCose = reg.getPublicKeyCose();
        if (!StringUtils.hasText(publicKeyCose)) {
            throw new MissingDataException("public-key");
        }

        // check duplicate
        WebAuthnUserCredential c = credentialsRepository.findByProviderAndUserHandleAndCredentialId(repositoryId,
                userHandle,
                credentialId);
        if (c != null) {
            throw new AlreadyRegisteredException();
        }

        // generate unique id
        // TODO evaluate secure key generator in place of uuid
        String id = UUID.randomUUID().toString();

        String transports = reg.getTransports();
        long signatureCount = reg.getSignatureCount();
        if (signatureCount < 0) {
            throw new InvalidDataException("signature-count");
        }

        String displayName = reg.getDisplayName();
        if (StringUtils.hasText(displayName)) {
            displayName = Jsoup.clean(displayName, Safelist.none());
        }

        // build model
        c = new WebAuthnUserCredential();
        c.setId(id);
        c.setProvider(repositoryId);

        c.setUsername(username);
        c.setUserHandle(userHandle);

        c.setCredentialId(credentialId);
        c.setDisplayName(displayName);
        c.setPublicKeyCose(publicKeyCose);
        c.setTransports(transports);
        c.setSignatureCount(signatureCount);
        c.setDiscoverable(reg.getDiscoverable());

        c.setAttestationObject(reg.getAttestationObject());
        c.setClientData(reg.getClientData());

        c.setStatus(CredentialsStatus.ACTIVE.getValue());

        logger.debug("add credential {} for {}", c.getCredentialId(), String.valueOf(userHandle));
        if (logger.isTraceEnabled()) {
            logger.trace("new credential: {}", String.valueOf(c));
        }

        c = credentialsRepository.saveAndFlush(c);
        c = credentialsRepository.detach(c);

        return c;
    }

    public WebAuthnUserCredential updateCredential(String userHandle, String credentialId,
            WebAuthnUserCredential reg) throws NoSuchCredentialException, RegistrationException {
        // fetch credential from repo via provided id
        WebAuthnUserCredential c = credentialsRepository.findByProviderAndUserHandleAndCredentialId(repositoryId,
                userHandle,
                credentialId);
        if (c == null) {
            throw new NoSuchCredentialException();
        }

        // extract data
        String displayName = reg.getDisplayName();
        if (StringUtils.hasText(displayName)) {
            displayName = Jsoup.clean(displayName, Safelist.none());
        }

        long signatureCount = reg.getSignatureCount();
        if (signatureCount < 0) {
            throw new InvalidDataException("signature-count");
        }
        // update allowed fields
        c.setDisplayName(displayName);
        c.setSignatureCount(signatureCount);

        logger.debug("update credential {} displayName {} signature count {}", c.getCredentialId(),
                String.valueOf(displayName), String.valueOf(signatureCount));
        if (logger.isTraceEnabled()) {
            logger.trace("update credential: {}", String.valueOf(c));
        }

        c = credentialsRepository.saveAndFlush(c);
        c = credentialsRepository.detach(c);

        return c;
    }

    public WebAuthnUserCredential updateCredentialCounter(String userHandle, String credentialId,
            long count) throws NoSuchCredentialException, RegistrationException {
        // fetch credential from repo via provided id
        WebAuthnUserCredential c = credentialsRepository.findByProviderAndUserHandleAndCredentialId(repositoryId,
                userHandle,
                credentialId);
        if (c == null) {
            throw new NoSuchCredentialException();
        }

        // allow only increment
        long signatureCount = c.getSignatureCount();
        if (count < signatureCount) {
            throw new InvalidDataException("signature-count");
        }

        // update field
        c.setSignatureCount(count);
        logger.debug("update credential {} signature count to {}", c.getCredentialId(), String.valueOf(count));

        c = credentialsRepository.saveAndFlush(c);
        c = credentialsRepository.detach(c);

        return c;
    }

    public void deleteCredential(String userHandle, String credentialId) {
        // fetch credential from repo via provided id
        WebAuthnUserCredential c = credentialsRepository.findByProviderAndUserHandleAndCredentialId(repositoryId,
                userHandle,
                credentialId);
        if (c != null) {
            logger.debug("delete credential {} with id {}", c.getCredentialId(), c.getId());
            credentialsRepository.delete(c);
        }
    }

    public WebAuthnUserCredential revokeCredential(String userHandle, String credentialId) throws NoSuchUserException {
        // fetch credential from repo via provided id
        WebAuthnUserCredential c = credentialsRepository.findByProviderAndUserHandleAndCredentialId(repositoryId,
                userHandle,
                credentialId);
        if (c == null) {
            throw new NoSuchUserException();
        }

        // we can transition from any status to revoked
        if (!STATUS_REVOKED.equals(c.getStatus())) {
            // update status
            c.setStatus(STATUS_REVOKED);

            logger.debug("revoke credential {}", c.getCredentialId());
            c = credentialsRepository.saveAndFlush(c);
        }

        c = credentialsRepository.detach(c);
        return c;
    }

    public void validateCredential(WebAuthnUserCredential reg) {
        // validate credentials
        if (!StringUtils.hasText(reg.getUserHandle())) {
            throw new MissingDataException("user-handle");
        }
        if (!StringUtils.hasText(reg.getCredentialId())) {
            throw new MissingDataException("credentials-id");
        }
        if (!StringUtils.hasText(reg.getPublicKeyCose())) {
            throw new MissingDataException("public-key");
        }
    }

    /*
     * Credentials service
     */

    @Override
    public WebAuthnUserCredential getCredentials(String username) throws NoSuchUserException {
        // not available as single
        return null;
    }

    @Override
    public WebAuthnUserCredential setCredentials(String username, UserCredentials cred) throws NoSuchUserException {
        if (!(cred instanceof WebAuthnUserCredential)) {
            throw new IllegalArgumentException("invalid credentials");
        }

        WebAuthnUserCredential credentials = (WebAuthnUserCredential) cred;
        validateCredential(credentials);

        if (!username.equals(credentials.getUsername())) {
            throw new IllegalArgumentException("invalid credentials");
        }

        // fetch user
        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // add as new credential, if id is available
        String userHandle = account.getUuid();
        String credentialsId = credentials.getCredentialId();
        return this.addCredential(userHandle, credentialsId, credentials);
    }

    @Override
    public void resetCredentials(String username) throws NoSuchUserException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void revokeCredentials(String username) throws NoSuchUserException {
        // fetch all credentials and revoke
        List<WebAuthnUserCredential> credentials = findCredentialsByUsername(username);
        if (!credentials.isEmpty()) {
            for (WebAuthnUserCredential c : credentials) {
                revokeCredential(c.getUserHandle(), c.getCredentialId());
            }
        }
    }

    @Override
    public void deleteCredentials(String username) throws NoSuchUserException {
        // fetch all credentials and delete
        List<WebAuthnUserCredential> credentials = findCredentialsByUsername(username);
        if (!credentials.isEmpty()) {
            for (WebAuthnUserCredential c : credentials) {
                deleteCredential(c.getUserHandle(), c.getCredentialId());
            }
        }
    }

    @Override
    public Collection<WebAuthnUserCredential> listCredentials(String username) throws NoSuchUserException {
        List<WebAuthnUserCredential> credentials = findCredentialsByUsername(username);

        // erase key data from registrations
        credentials.forEach(c -> c.eraseCredentials());

        return credentials;
    }

    @Override
    public WebAuthnUserCredential getCredentials(String username, String credentialsId) throws NoSuchUserException {
        Optional<WebAuthnUserCredential> cred = findCredentialsByUsername(username).stream()
                .filter(c -> credentialsId.equals(c.getCredentialId()))
                .findFirst();
        if (cred.isEmpty()) {
            throw new NoSuchUserException();
        }
        WebAuthnUserCredential credential = cred.get();

        // erase key data from registration
        credential.eraseCredentials();

        return credential;
    }

    @Override
    public WebAuthnUserCredential setCredentials(String username, String credentialsId, UserCredentials cred)
            throws NoSuchUserException, RegistrationException, NoSuchCredentialException {
        if (!(cred instanceof WebAuthnUserCredential)) {
            throw new IllegalArgumentException("invalid credentials");
        }

        WebAuthnUserCredential credentials = (WebAuthnUserCredential) cred;
        validateCredential(credentials);

        if (!credentialsId.equals(credentials.getCredentialId())) {
            throw new IllegalArgumentException("invalid credentials");
        }

        if (!username.equals(credentials.getUsername())) {
            throw new IllegalArgumentException("invalid credentials");
        }

        // fetch user
        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        String userHandle = account.getUuid();

        // update existing credential or add as new
        WebAuthnUserCredential c = findCredentialByUserHandleAndCredentialId(userHandle, credentialsId);
        if (c == null) {
            return this.addCredential(userHandle, credentialsId, credentials);
        } else {
            return this.updateCredential(userHandle, credentialsId, credentials);
        }
    }

    @Override
    public void resetCredentials(String username, String credentialsId) throws NoSuchUserException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void revokeCredentials(String username, String credentialsId) throws NoSuchUserException {
        Optional<WebAuthnUserCredential> cred = findCredentialsByUsername(username).stream()
                .filter(c -> credentialsId.equals(c.getCredentialId()))
                .findFirst();
        if (cred.isEmpty()) {
            throw new NoSuchUserException();
        }
        WebAuthnUserCredential credential = cred.get();

        // revoke
        revokeCredential(credential.getUserHandle(), credential.getCredentialId());
    }

    @Override
    public void deleteCredentials(String username, String credentialsId) throws NoSuchUserException {
        Optional<WebAuthnUserCredential> cred = findCredentialsByUsername(username).stream()
                .filter(c -> credentialsId.equals(c.getCredentialId()))
                .findFirst();
        if (cred.isEmpty()) {
            throw new NoSuchUserException();
        }
        WebAuthnUserCredential credential = cred.get();

        // delete
        deleteCredential(credential.getUserHandle(), credential.getCredentialId());
    }

    @Override
    public String getSetUrl() throws NoSuchUserException {
        return null;
    }

    @Override
    public String getResetUrl() {
        return null;
    }

    @Override
    public AbstractProviderConfig getConfig() {
        return config;
    }

    @Override
    public String getName() {
        return config.getName();
    }

    @Override
    public String getDescription() {
        return config.getDescription();
    }

    /*
     * Status codes
     */
    private static final String STATUS_ACTIVE = CredentialsStatus.ACTIVE.getValue();
    private static final String STATUS_REVOKED = CredentialsStatus.REVOKED.getValue();
//    private static final String STATUS_EXPIRED = CredentialsStatus.EXPIRED.getValue();

}
