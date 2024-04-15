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

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.base.provider.AbstractProvider;
import it.smartcommunitylab.aac.common.InvalidDataException;
import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.internal.model.CredentialsStatus;
import it.smartcommunitylab.aac.internal.model.InternalUserAccount;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnUserCredential;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnJpaUserCredentialsService;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnUserHandleService;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Transactional
public class WebAuthnIdentityCredentialsService extends AbstractProvider<WebAuthnUserCredential> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String STATUS_ACTIVE = CredentialsStatus.ACTIVE.getValue();

    private final UserAccountService<InternalUserAccount> accountService;
    private final WebAuthnJpaUserCredentialsService credentialsService;
    private final WebAuthnUserHandleService userHandleService;

    private final WebAuthnIdentityProviderConfig config;
    private final String repositoryId;

    private ResourceEntityService resourceService;

    public WebAuthnIdentityCredentialsService(
        String providerId,
        UserAccountService<InternalUserAccount> accountService,
        WebAuthnJpaUserCredentialsService credentialsService,
        WebAuthnIdentityProviderConfig config,
        String realm
    ) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, providerId, realm);
        Assert.notNull(accountService, "account service is mandatory");
        Assert.notNull(credentialsService, "webauthn credentials service is mandatory");
        Assert.notNull(config, "config is mandatory");

        this.accountService = accountService;
        this.credentialsService = credentialsService;

        this.config = config;

        // repositoryId from config
        this.repositoryId = this.config.getRepositoryId();

        // build service
        this.userHandleService = new WebAuthnUserHandleService(accountService);
    }

    public void setResourceService(ResourceEntityService resourceService) {
        this.resourceService = resourceService;
    }

    public String getUuidFromUserHandle(String userHandle) throws NoSuchUserException {
        String username = userHandleService.getUsernameForUserHandle(repositoryId, userHandle);
        if (username == null) {
            throw new NoSuchUserException();
        }

        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        if (!StringUtils.hasText(account.getUuid())) {
            throw new NoSuchUserException();
        }

        return account.getUuid();
    }

    @Transactional(readOnly = true)
    public WebAuthnUserCredential findCredential(String userHandle, String credentialId) throws NoSuchUserException {
        InternalUserAccount account = accountService.findAccountByUuid(getUuidFromUserHandle(userHandle));
        if (account == null) {
            throw new NoSuchUserException();
        }

        return credentialsService.findCredentialByUserHandleAndCredentialId(repositoryId, userHandle, credentialId);
    }

    public WebAuthnUserCredential updateCredentialCounter(String userHandle, String credentialId, long count)
        throws RegistrationException, NoSuchCredentialException {
        WebAuthnUserCredential c = credentialsService.findCredentialByUserHandleAndCredentialId(
            repositoryId,
            userHandle,
            credentialId
        );
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

        // register usage date
        c.setLastUsedDate(new Date());

        logger.debug(
            "update credential {} signature count to {} on date {}",
            c.getCredentialId(),
            String.valueOf(count),
            String.valueOf(c.getLastUsedDate())
        );

        c = credentialsService.updateCredentials(repositoryId, c.getId(), c);
        return c;
    }
    // public void deleteCredentialsByUsername(String username) {
    //     logger.debug("delete all credentials for account {}", String.valueOf(username));

    //     // fetch all to collect ids
    //     List<WebAuthnUserCredential> passwords = credentialsService.findCredentialsByAccount(repositoryId, username);

    //     // delete in batch
    //     Set<String> ids = passwords.stream().map(p -> p.getId()).collect(Collectors.toSet());
    //     credentialsService.deleteAllCredentials(repositoryId, ids);

    //     if (resourceService != null) {
    //         // remove resources
    //         try {
    //             // delete in batch
    //             Set<String> uuids = passwords.stream().map(p -> p.getUuid()).collect(Collectors.toSet());
    //             resourceService.deleteAllResourceEntities(uuids);
    //         } catch (RuntimeException re) {
    //             logger.error("error removing resources: {}", re.getMessage());
    //         }
    //     }
    // }
}
