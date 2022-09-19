package it.smartcommunitylab.aac.webauthn.provider;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.AuthenticatorTransport;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.MissingDataException;
import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.base.AbstractConfigurableProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableCredentialsService;
import it.smartcommunitylab.aac.core.model.UserCredentials;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.core.provider.UserCredentialsService;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.webauthn.model.AttestationResponse;
import it.smartcommunitylab.aac.webauthn.model.CredentialCreationInfo;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnRegistrationRequest;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnRegistrationStartRequest;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserCredential;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnRegistrationRpService;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnUserCredentialsService;

@Transactional
public class WebAuthnCredentialsService
        extends
        AbstractConfigurableProvider<WebAuthnUserCredential, ConfigurableCredentialsService, WebAuthnCredentialsServiceConfigMap, WebAuthnCredentialsServiceConfig>
        implements
        UserCredentialsService<WebAuthnUserCredential, WebAuthnCredentialsServiceConfigMap, WebAuthnCredentialsServiceConfig> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    // services
    private final UserAccountService<InternalUserAccount> accountService;
    private final WebAuthnUserCredentialsService credentialsService;
    private final WebAuthnRegistrationRpService rpService;

    // provider configuration
    private final WebAuthnCredentialsServiceConfig config;
    private final String repositoryId;

    public WebAuthnCredentialsService(String providerId,
            UserAccountService<InternalUserAccount> userAccountService,
            WebAuthnUserCredentialsService credentialsService, WebAuthnRegistrationRpService rpService,
            WebAuthnCredentialsServiceConfig providerConfig,
            String realm) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, providerId, realm, providerConfig);
        Assert.notNull(userAccountService, "user account service is mandatory");
        Assert.notNull(credentialsService, "credentials service is mandatory");
        Assert.notNull(rpService, "webauthn rp service is mandatory");
        Assert.notNull(providerConfig, "provider config is mandatory");

        this.repositoryId = providerConfig.getRepositoryId();
        logger.debug("create webauthn credentials service with id {} repository {}", String.valueOf(providerId),
                repositoryId);
        this.config = providerConfig;

        this.accountService = userAccountService;
        this.credentialsService = credentialsService;
        this.rpService = rpService;
    }

    @Override
    public String getName() {
        return config.getName();
    }

    @Override
    public String getDescription() {
        return config.getDescription();
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_CREDENTIALS;
    }

    private void validateCredential(WebAuthnUserCredential reg) {
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
     * WebAuthn operations
     */

    public WebAuthnRegistrationRequest startRegistration(String username, WebAuthnRegistrationStartRequest reg)
            throws NoSuchUserException, RegistrationException {
        // fetch user
        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        String displayName = reg.getDisplayName();
        if (StringUtils.hasText(displayName)) {
            displayName = Jsoup.clean(displayName, Safelist.none());
        }

        try {
            // build info via service
            CredentialCreationInfo info = rpService.startRegistration(getProvider(), username, displayName);
            String userHandle = new String(info.getUserHandle().getBytes());

            // build a new request
            WebAuthnRegistrationRequest request = new WebAuthnRegistrationRequest(userHandle);
            request.setStartRequest(new WebAuthnRegistrationStartRequest(username, displayName));
            request.setCredentialCreationInfo(info);

            return request;
        } catch (NoSuchProviderException e) {
            // error
            throw new RegistrationException();
        }
    }

    public WebAuthnRegistrationRequest finishRegistration(
            String username, WebAuthnRegistrationRequest request,
            PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc)
            throws RegistrationException, NoSuchUserException {
        // fetch user
        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            throw new NoSuchUserException();
        }
        try {
            RegistrationResult result = rpService.finishRegistration(getProvider(), request, pkc);
            request.setRegistrationResult(result);

            return request;
        } catch (NoSuchProviderException e) {
            // error
            throw new RegistrationException();
        }

    }

    public WebAuthnUserCredential saveRegistration(String username, WebAuthnRegistrationRequest request)
            throws NoSuchUserException {
        logger.debug("save registration for user {}", StringUtils.trimAllWhitespace(username));

        // fetch user
        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        String userHandle = request.getUserHandle();
        RegistrationResult result = request.getRegistrationResult();
        AttestationResponse attestation = request.getAttestationResponse();

        if (!account.getUuid().equals(userHandle)) {
            throw new IllegalArgumentException("user mismatch");
        }

        if (result == null || attestation == null) {
            throw new RegistrationException();
        }

        // parse pkc from attestation
        PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc = null;
        try {
            pkc = PublicKeyCredential.parseRegistrationResponseJson(attestation.getAttestation());
        } catch (IOException e) {
        }

        if (logger.isTraceEnabled()) {
            logger.trace("pkc {}", String.valueOf(pkc));
        }

        if (pkc == null) {
            throw new RegistrationException();
        }

        // create a new credential in repository for the result
        WebAuthnUserCredential credential = new WebAuthnUserCredential();
        credential.setUsername(username);
        credential.setCredentialId(result.getKeyId().getId().getBase64Url());
        credential.setUserHandle(userHandle);

        credential.setDisplayName(request.getStartRequest().getDisplayName());
        credential.setPublicKeyCose(result.getPublicKeyCose().getBase64());
        credential.setSignatureCount(result.getSignatureCount());

        if (result.getKeyId().getTransports().isPresent()) {
            Set<AuthenticatorTransport> transports = result.getKeyId().getTransports().get();
            List<String> transportCodes = transports.stream().map(t -> t.getId()).collect(Collectors.toList());
            credential.setTransports(StringUtils.collectionToCommaDelimitedString(transportCodes));
        }

        Boolean discoverable = result.isDiscoverable().isPresent() ? result.isDiscoverable().get() : null;
        credential.setDiscoverable(discoverable);

        // TODO add support for additional fields in registration
        credential.setAttestationObject(pkc.getResponse().getAttestation().getBytes().getBase64());
        credential.setClientData(pkc.getResponse().getClientDataJSON().getBase64());

        // register as new
        logger.debug("register credential {} for user {} via userHandle {}", credential.getCredentialId(),
                StringUtils.trimAllWhitespace(username), userHandle);

        credential = setCredentials(username, credential);

        if (logger.isTraceEnabled()) {
            logger.trace("credential {}: {}", credential.getCredentialId(), String.valueOf(credential));
        }

        return credential;
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
        return credentialsService.addCredential(repositoryId, userHandle, credentialsId, credentials);
    }

    @Override
    public void resetCredentials(String username) throws NoSuchUserException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void revokeCredentials(String username) throws NoSuchUserException {
        // fetch all credentials and revoke
        List<WebAuthnUserCredential> credentials = credentialsService.findCredentialsByUsername(repositoryId, username);
        if (!credentials.isEmpty()) {
            for (WebAuthnUserCredential c : credentials) {
                credentialsService.revokeCredential(repositoryId, c.getUserHandle(), c.getCredentialId());
            }
        }
    }

    @Override
    public void deleteCredentials(String username) throws NoSuchUserException {
        // fetch all credentials and delete
        List<WebAuthnUserCredential> credentials = credentialsService.findCredentialsByUsername(repositoryId, username);
        if (!credentials.isEmpty()) {
            for (WebAuthnUserCredential c : credentials) {
                credentialsService.deleteCredential(repositoryId, c.getUserHandle(), c.getCredentialId());
            }
        }
    }

    @Override
    public Collection<WebAuthnUserCredential> listCredentials(String username) throws NoSuchUserException {
        List<WebAuthnUserCredential> credentials = credentialsService.findCredentialsByUsername(repositoryId, username);

        // erase key data from registrations
        credentials.forEach(c -> c.eraseCredentials());

        return credentials;
    }

    @Override
    public WebAuthnUserCredential getCredentials(String username, String credentialsId) throws NoSuchUserException {
        Optional<WebAuthnUserCredential> cred = credentialsService.findCredentialsByUsername(repositoryId, username)
                .stream()
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
        WebAuthnUserCredential c = credentialsService.findCredentialByUserHandleAndCredentialId(repositoryId,
                userHandle, credentialsId);
        if (c == null) {
            return credentialsService.addCredential(repositoryId, userHandle, credentialsId, credentials);
        } else {
            return credentialsService.updateCredential(repositoryId, userHandle, credentialsId, credentials);
        }
    }

    @Override
    public void resetCredentials(String username, String credentialsId) throws NoSuchUserException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void revokeCredentials(String username, String credentialsId) throws NoSuchUserException {
        Optional<WebAuthnUserCredential> cred = credentialsService.findCredentialsByUsername(repositoryId, username)
                .stream()
                .filter(c -> credentialsId.equals(c.getCredentialId()))
                .findFirst();
        if (cred.isEmpty()) {
            throw new NoSuchUserException();
        }
        WebAuthnUserCredential credential = cred.get();

        // revoke
        credentialsService.revokeCredential(repositoryId, credential.getUserHandle(), credential.getCredentialId());
    }

    @Override
    public void deleteCredentials(String username, String credentialsId) throws NoSuchUserException {
        Optional<WebAuthnUserCredential> cred = credentialsService.findCredentialsByUsername(repositoryId, username)
                .stream()
                .filter(c -> credentialsId.equals(c.getCredentialId()))
                .findFirst();
        if (cred.isEmpty()) {
            throw new NoSuchUserException();
        }
        WebAuthnUserCredential credential = cred.get();

        // delete
        credentialsService.deleteCredential(repositoryId, credential.getUserHandle(), credential.getCredentialId());
    }

    @Override
    public String getSetUrl() throws NoSuchUserException {
        return null;
    }

    @Override
    public String getResetUrl() {
        return null;
    }

}
