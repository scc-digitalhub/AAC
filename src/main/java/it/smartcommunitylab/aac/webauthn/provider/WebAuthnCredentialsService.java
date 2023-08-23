/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.webauthn.provider;

import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.AuthenticatorTransport;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.common.InvalidDataException;
import it.smartcommunitylab.aac.common.MissingDataException;
import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.credentials.base.AbstractCredentialsService;
import it.smartcommunitylab.aac.credentials.model.EditableUserCredentials;
import it.smartcommunitylab.aac.credentials.model.UserCredentials;
import it.smartcommunitylab.aac.internal.model.CredentialsStatus;
import it.smartcommunitylab.aac.internal.model.InternalUserAccount;
import it.smartcommunitylab.aac.webauthn.model.AttestationResponse;
import it.smartcommunitylab.aac.webauthn.model.CredentialCreationInfo;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnEditableUserCredential;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnRegistrationRequest;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnRegistrationStartRequest;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnUserCredential;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnRegistrationRpService;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnUserCredentialsService;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnUserHandleService;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Transactional
public class WebAuthnCredentialsService
    extends AbstractCredentialsService<WebAuthnUserCredential, WebAuthnEditableUserCredential, InternalUserAccount, WebAuthnIdentityProviderConfigMap, WebAuthnCredentialsServiceConfig> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // services
    private final WebAuthnUserCredentialsService credentialService;
    private final WebAuthnUserHandleService userHandleService;
    private final WebAuthnRegistrationRpService rpService;

    public WebAuthnCredentialsService(
        String providerId,
        UserAccountService<InternalUserAccount> userAccountService,
        WebAuthnUserCredentialsService credentialsService,
        WebAuthnRegistrationRpService rpService,
        WebAuthnCredentialsServiceConfig providerConfig,
        String realm
    ) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, providerId, userAccountService, credentialsService, providerConfig, realm);
        Assert.notNull(credentialsService, "credentials service is mandatory");
        Assert.notNull(rpService, "webauthn rp service is mandatory");

        this.credentialService = credentialsService;
        this.rpService = rpService;

        // build service
        this.userHandleService = new WebAuthnUserHandleService(userAccountService);
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
        String username,
        WebAuthnRegistrationRequest request,
        PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc
    ) throws RegistrationException, NoSuchUserException {
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

    public WebAuthnUserCredential saveRegistration(
        String username,
        String displayName,
        WebAuthnRegistrationRequest request
    ) throws NoSuchUserException, RegistrationException {
        logger.debug("save registration for user {}", StringUtils.trimAllWhitespace(username));

        if (request == null) {
            throw new RegistrationException();
        }

        // fetch user
        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        String userHandle = request.getUserHandle();
        RegistrationResult result = request.getRegistrationResult();
        AttestationResponse attestation = request.getAttestationResponse();
        if (displayName == null) {
            displayName = request.getStartRequest().getDisplayName();
        }

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
        } catch (IOException e) {}

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

        credential.setDisplayName(displayName);
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
        logger.debug(
            "register credential {} for user {} via userHandle {}",
            credential.getCredentialId(),
            StringUtils.trimAllWhitespace(username),
            userHandle
        );

        credential = addCredential(username, null, credential);

        if (logger.isTraceEnabled()) {
            logger.trace("credential {}: {}", credential.getCredentialId(), String.valueOf(credential));
        }

        return credential;
    }

    /*
     * Credentials service
     */

    @Override
    public WebAuthnUserCredential addCredential(String accountId, String credentialsId, UserCredentials cred)
        throws NoSuchUserException, RegistrationException {
        if (cred == null) {
            throw new RegistrationException();
        }

        Assert.isInstanceOf(
            WebAuthnUserCredential.class,
            cred,
            "registration must be an instance of webauthn user credential"
        );
        WebAuthnUserCredential reg = (WebAuthnUserCredential) cred;

        logger.debug("add credential for account {}", String.valueOf(accountId));
        if (logger.isTraceEnabled()) {
            logger.trace("credentials: {}", String.valueOf(reg));
        }

        validateCredential(reg);

        if (!accountId.equals(reg.getUsername())) {
            throw new IllegalArgumentException("invalid credentials");
        }

        // fetch user
        InternalUserAccount account = accountService.findAccountById(repositoryId, accountId);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // fetch user handle
        String userHandle = userHandleService.getUserHandleForUsername(repositoryId, accountId);
        if (userHandle == null) {
            throw new NoSuchUserException();
        }

        // add as new credential, if id is available
        String credentialId = reg.getCredentialId();
        if (!StringUtils.hasText(credentialId)) {
            throw new MissingDataException("credential-id");
        }

        // check duplicate
        WebAuthnUserCredential c = credentialService.findCredentialByUserHandleAndCredentialId(
            repositoryId,
            userHandle,
            credentialId
        );
        if (c != null) {
            throw new AlreadyRegisteredException();
        }

        // extract relevant data
        String username = reg.getUsername();
        if (!StringUtils.hasText(username)) {
            throw new MissingDataException("username");
        }

        String publicKeyCose = reg.getPublicKeyCose();
        if (!StringUtils.hasText(publicKeyCose)) {
            throw new MissingDataException("public-key");
        }

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
        c.setRepositoryId(repositoryId);

        c.setUsername(account.getUsername());
        c.setUserId(account.getUserId());
        c.setRealm(account.getRealm());

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

        // save
        WebAuthnUserCredential newCred = super.addCredential(accountId, credentialsId, c);

        // map to ourselves
        newCred.setProvider(getProvider());

        // clear value for extra safety
        newCred.eraseCredentials();

        return newCred;
    }

    @Override
    public WebAuthnUserCredential setCredential(String credentialsId, UserCredentials uc)
        throws RegistrationException, NoSuchCredentialException {
        Assert.isInstanceOf(
            WebAuthnUserCredential.class,
            uc,
            "registration must be an instance of webauthn user credential"
        );
        WebAuthnUserCredential reg = (WebAuthnUserCredential) uc;

        logger.debug("add credential with id {}", String.valueOf(credentialsId));
        if (logger.isTraceEnabled()) {
            logger.trace("credentials: {}", String.valueOf(reg));
        }

        validateCredential(reg);

        WebAuthnUserCredential cred = credentialService.findCredentialsById(repositoryId, credentialsId);
        if (cred == null) {
            throw new NoSuchCredentialException();
        }

        // fetch user
        String accountId = cred.getAccountId();
        InternalUserAccount account = accountService.findAccountById(repositoryId, accountId);
        if (account == null) {
            throw new NoSuchCredentialException();
        }

        // update only allowed fields
        cred.setDisplayName(reg.getDisplayName());

        cred = credentialService.updateCredentials(repositoryId, cred.getId(), cred);

        // map to ourselves
        cred.setProvider(getProvider());

        // clear value for extra safety
        cred.eraseCredentials();

        return cred;
    }

    /*
     * Editable
     * TODO split
     */

    private WebAuthnEditableUserCredential toEditable(WebAuthnUserCredential cred) {
        WebAuthnEditableUserCredential ed = new WebAuthnEditableUserCredential(getProvider(), cred.getUuid());
        ed.setCredentialsId(cred.getCredentialsId());
        ed.setUserId(cred.getUserId());
        ed.setUsername(cred.getUsername());
        ed.setUserHandle(cred.getUserHandle());
        ed.setDisplayName(cred.getDisplayName());
        ed.setCreateDate(cred.getCreateDate());
        ed.setModifiedDate(cred.getCreateDate());
        ed.setLastUsedDate(cred.getLastUsedDate());

        return ed;
    }

    @Override
    public Collection<WebAuthnEditableUserCredential> listEditableCredentials(String accountId) {
        // fetch ALL active
        List<WebAuthnUserCredential> credentials = credentialService
            .findCredentialsByAccount(repositoryId, accountId)
            .stream()
            .filter(c -> STATUS_ACTIVE.equals(c.getStatus()))
            .collect(Collectors.toList());

        return credentials.stream().map(c -> toEditable(c)).collect(Collectors.toList());
    }

    @Override
    public Collection<WebAuthnEditableUserCredential> listEditableCredentialsByUser(String userId) {
        // fetch ALL active
        List<WebAuthnUserCredential> credentials = credentialService
            .findCredentialsByUser(repositoryId, userId)
            .stream()
            .filter(c -> STATUS_ACTIVE.equals(c.getStatus()))
            .collect(Collectors.toList());

        return credentials.stream().map(c -> toEditable(c)).collect(Collectors.toList());
    }

    @Override
    public WebAuthnEditableUserCredential getEditableCredential(String credentialId) throws NoSuchCredentialException {
        // get as editable
        WebAuthnUserCredential cred = getCredential(credentialId);
        return toEditable(cred);
    }

    @Override
    public void deleteEditableCredential(String credentialId) throws NoSuchCredentialException {
        deleteCredential(credentialId);
    }

    @Override
    public WebAuthnEditableUserCredential editEditableCredential(String credentialId, EditableUserCredentials uc)
        throws RegistrationException, NoSuchCredentialException {
        if (uc == null) {
            throw new RegistrationException();
        }

        Assert.isInstanceOf(
            WebAuthnEditableUserCredential.class,
            uc,
            "registration must be an instance of webauthn credential"
        );
        WebAuthnEditableUserCredential reg = (WebAuthnEditableUserCredential) uc;

        // we can edit only display name
        String displayName = reg.getDisplayName();
        if (StringUtils.hasText(displayName)) {
            displayName = Jsoup.clean(displayName, Safelist.none());
        }

        WebAuthnUserCredential cred = credentialService.findCredentialsById(repositoryId, credentialId);
        if (cred == null) {
            throw new NoSuchCredentialException();
        }

        // fetch user
        String accountId = cred.getAccountId();
        InternalUserAccount account = accountService.findAccountById(repositoryId, accountId);
        if (account == null) {
            throw new NoSuchCredentialException();
        }

        // update only allowed fields
        cred.setDisplayName(displayName);

        cred = credentialService.updateCredentials(repositoryId, cred.getId(), cred);

        // map to ourselves
        cred.setProvider(getProvider());

        // clear value for extra safety
        cred.eraseCredentials();

        return toEditable(cred);
    }

    public WebAuthnEditableUserCredential registerEditableCredential(
        String accountId,
        WebAuthnEditableUserCredential credentials,
        WebAuthnRegistrationRequest request
    ) throws RegistrationException, NoSuchUserException {
        if (credentials == null || request == null) {
            throw new RegistrationException();
        }

        // fetch attestation from registration
        String attestation = credentials.getAttestation();
        if (!StringUtils.hasText(attestation)) {
            throw new RegistrationException("missing attestation");
        }

        // we can edit only display name
        String displayName = credentials.getDisplayName();
        if (StringUtils.hasText(displayName)) {
            displayName = Jsoup.clean(displayName, Safelist.none());
        } else {
            displayName = accountId;
        }

        // update request and process
        AttestationResponse attestationResponse = new AttestationResponse();
        attestationResponse.setAttestation(attestation);
        request.setAttestationResponse(attestationResponse);

        // parse body
        PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc = null;
        try {
            pkc = PublicKeyCredential.parseRegistrationResponseJson(attestation);
        } catch (IOException e) {}

        request = this.finishRegistration(accountId, request, pkc);

        // save successful registration as credential
        WebAuthnUserCredential credential = saveRegistration(accountId, displayName, request);

        return toEditable(credential);
    }

    @Override
    public String getRegisterUrl() {
        return "/webauthn/credentials/register/" + getProvider();
    }

    @Override
    public String getEditUrl(String credentialsId) throws NoSuchCredentialException {
        WebAuthnUserCredential cred = credentialService.findCredentialsById(repositoryId, credentialsId);
        if (cred == null) {
            throw new NoSuchCredentialException();
        }

        return "/webauthn/credentials/edit/" + getProvider() + "/" + credentialsId;
    }

    /*
     * helpers
     */
    private void validateCredential(WebAuthnUserCredential reg) throws RegistrationException {
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
}
