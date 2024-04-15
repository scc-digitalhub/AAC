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

package it.smartcommunitylab.aac.password.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.base.provider.AbstractProvider;
import it.smartcommunitylab.aac.common.InvalidDataException;
import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.service.ResourceEntityService;
import it.smartcommunitylab.aac.crypto.PasswordHash;
import it.smartcommunitylab.aac.internal.model.CredentialsStatus;
import it.smartcommunitylab.aac.internal.model.InternalUserAccount;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.oauth.common.SecureStringKeyGenerator;
import it.smartcommunitylab.aac.password.PasswordIdentityAuthority;
import it.smartcommunitylab.aac.password.model.InternalUserPassword;
import it.smartcommunitylab.aac.password.service.InternalPasswordJpaUserCredentialsService;
import it.smartcommunitylab.aac.realms.service.RealmService;
import it.smartcommunitylab.aac.utils.MailService;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

@Transactional
public class PasswordIdentityCredentialsService extends AbstractProvider<InternalUserPassword> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String STATUS_ACTIVE = CredentialsStatus.ACTIVE.getValue();
    private static final String STATUS_INACTIVE = CredentialsStatus.INACTIVE.getValue();

    private final InternalPasswordJpaUserCredentialsService passwordService;
    private final UserAccountService<InternalUserAccount> accountService;

    private final PasswordIdentityProviderConfig config;
    private final String repositoryId;

    private PasswordHash hasher;
    private StringKeyGenerator keyGenerator;

    private RealmService realmService;
    private MailService mailService;
    private RealmAwareUriBuilder uriBuilder;
    private ResourceEntityService resourceService;

    public PasswordIdentityCredentialsService(
        String providerId,
        UserAccountService<InternalUserAccount> accountService,
        InternalPasswordJpaUserCredentialsService passwordService,
        PasswordIdentityProviderConfig config,
        String realm
    ) {
        super(SystemKeys.AUTHORITY_PASSWORD, providerId, realm);
        Assert.notNull(accountService, "account service is mandatory");
        Assert.notNull(passwordService, "password service is mandatory");
        Assert.notNull(config, "config is mandatory");

        this.config = config;

        this.accountService = accountService;
        this.passwordService = passwordService;

        // repositoryId from config
        this.repositoryId = config.getRepositoryId();

        this.hasher = new PasswordHash();

        // use a secure string generator for keys of length 20
        keyGenerator = new SecureStringKeyGenerator(20);
    }

    public void setKeyGenerator(StringKeyGenerator keyGenerator) {
        this.keyGenerator = keyGenerator;
    }

    public void setHasher(PasswordHash hasher) {
        Assert.notNull(hasher, "password hasher can not be null");
        this.hasher = hasher;
    }

    public void setRealmService(RealmService realmService) {
        this.realmService = realmService;
    }

    public void setMailService(MailService mailService) {
        this.mailService = mailService;
    }

    public void setUriBuilder(RealmAwareUriBuilder uriBuilder) {
        this.uriBuilder = uriBuilder;
    }

    public void setResourceService(ResourceEntityService resourceService) {
        this.resourceService = resourceService;
    }

    // @Override
    // public String getType() {
    //     return SystemKeys.RESOURCE_CREDENTIALS;
    // }

    // @Transactional(readOnly = true)
    // public List<InternalUserPassword> findPassword(String username) throws NoSuchUserException {
    //     InternalUserAccount account = accountService.findAccountById(repositoryId, username);
    //     if (account == null) {
    //         throw new NoSuchUserException();
    //     }

    //     // fetch all active passwords
    //     return passwordService
    //         .findCredentialsByAccount(repositoryId, username)
    //         .stream()
    //         .filter(c -> STATUS_ACTIVE.equals(c.getStatus()))
    //         .map(p -> {
    //             // map to ourselves
    //             p.setProvider(getProvider());

    //             // password are encrypted, but clear value for extra safety
    //             p.eraseCredentials();
    //             return p;
    //         })
    //         .collect(Collectors.toList());
    // }

    public boolean verifyPassword(String username, String password) throws NoSuchUserException {
        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // fetch ALL active + non expired credentials from same user
        //NOTE: username in password should be dropped
        String userId = account.getUserId();
        List<InternalUserPassword> credentials = passwordService
            .findCredentialsByUser(repositoryId, userId)
            .stream()
            .filter(c -> STATUS_ACTIVE.equals(c.getStatus()) && !c.isExpired())
            .collect(Collectors.toList());

        // pick any match on hashed password
        return credentials
            .stream()
            .anyMatch(c -> {
                try {
                    return hasher.validatePassword(password, c.getPassword());
                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    throw new SystemException(e.getMessage());
                }
            });
    }

    //TODO remove from here! credentials management should go via credentials services
    public InternalUserPassword resetPassword(String username) throws NoSuchUserException {
        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            throw new NoSuchUserException();
        }
        try {
            // fetch first active password from same user
            //NOTE: username in password should be dropped
            String userId = account.getUserId();
            InternalUserPassword pass = passwordService
                .findCredentialsByUser(repositoryId, userId)
                .stream()
                .filter(c -> STATUS_ACTIVE.equals(c.getStatus()))
                .findFirst()
                .orElse(null);

            if (pass == null) {
                // generate and set active a temporary password
                String value = keyGenerator.generateKey();
                // encode password
                String hash = hasher.createHash(value);

                // create password already hashed
                String id = UUID.randomUUID().toString();
                InternalUserPassword newPassword = new InternalUserPassword(getRealm(), id);
                newPassword.setRepositoryId(repositoryId);
                newPassword.setProvider(getProvider());

                // newPassword.setUsername(username);
                newPassword.setUserId(userId);

                newPassword.setPassword(hash);
                newPassword.setChangeOnFirstAccess(true);
                newPassword.setExpirationDate(null);

                pass = passwordService.addCredentials(repositoryId, newPassword.getId(), newPassword);

                if (resourceService != null) {
                    // register
                    resourceService.addResourceEntity(
                        pass.getUuid(),
                        SystemKeys.RESOURCE_CREDENTIALS,
                        getAuthority(),
                        getProvider(),
                        pass.getId()
                    );
                }
            }

            // generate and set a reset key
            String resetKey = keyGenerator.generateKey();

            // we set deadline as +N seconds
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, config.getPasswordResetValidity());

            pass.setResetDeadline(calendar.getTime());
            pass.setResetKey(resetKey);

            pass = passwordService.updateCredentials(repositoryId, pass.getId(), pass);

            // map to ourselves
            pass.setProvider(getProvider());

            // send mail
            try {
                sendResetMail(account, pass.getResetKey(), pass.getResetDeadline());
            } catch (Exception e) {
                logger.error(e.getMessage());
            }

            // password are encrypted, but clear value for extra safety
            pass.eraseCredentials();

            return pass;
        } catch (
            RegistrationException | NoSuchCredentialException | NoSuchAlgorithmException | InvalidKeySpecException e
        ) {
            logger.error("error resetting password for {}: {}", String.valueOf(username), e.getMessage());
            throw new SystemException(e.getMessage());
        }
    }

    public InternalUserPassword confirmReset(String resetKey) throws NoSuchCredentialException, RegistrationException {
        if (!StringUtils.hasText(resetKey)) {
            throw new IllegalArgumentException("empty-key");
        }
        InternalUserPassword pass = passwordService.findCredentialsByResetKey(repositoryId, resetKey);
        if (pass == null) {
            throw new NoSuchCredentialException();
        }

        // validate key, we do it simple
        boolean isValid = false;

        // password must be active, can't reset inactive
        boolean isActive = STATUS_ACTIVE.equals(pass.getStatus());
        if (!isActive) {
            logger.error("invalid key, inactive");
            throw new InvalidDataException("key");
        }

        // validate key match
        // useless check since we fetch account with key as input..
        boolean isMatch = resetKey.equals(pass.getResetKey());

        if (!isMatch) {
            logger.error("invalid key, not matching");
            throw new InvalidDataException("key");
        }

        // validate deadline
        Calendar calendar = Calendar.getInstance();
        if (pass.getResetDeadline() == null) {
            logger.error("corrupt or used key, missing deadline");
            // do not leak reason
            throw new InvalidDataException("key");
        }

        boolean isExpired = calendar.after(pass.getResetDeadline());

        if (isExpired) {
            logger.error("expired key on " + String.valueOf(pass.getResetDeadline()));
            // do not leak reason
            throw new InvalidDataException("key");
        }

        isValid = isActive && isMatch && !isExpired;

        if (!isValid) {
            throw new InvalidDataException("key");
        }

        // we clear keys and reset password to lock login
        pass.setResetDeadline(null);
        pass.setResetKey(null);

        // users need to change the password during this session or reset again
        // we want to lock login with old password from now on
        pass.setStatus(STATUS_INACTIVE);

        pass = passwordService.updateCredentials(resetKey, pass.getId(), pass);

        // map to ourselves
        pass.setProvider(getProvider());

        // password are encrypted, but clear value for extra safety
        pass.eraseCredentials();

        return pass;
    }

    public InternalUserPassword verifyReset(String resetKey) throws NoSuchCredentialException, InvalidDataException {
        if (!StringUtils.hasText(resetKey)) {
            throw new IllegalArgumentException("empty-key");
        }

        InternalUserPassword pass = passwordService.findCredentialsByResetKey(repositoryId, resetKey);
        if (pass == null) {
            throw new NoSuchCredentialException();
        }

        // validate key, we do it simple
        boolean isValid = false;

        // password must be active, can't reset inactive
        boolean isActive = STATUS_ACTIVE.equals(pass.getStatus());
        if (!isActive) {
            logger.error("invalid key, inactive");
            throw new InvalidDataException("key");
        }

        // validate key match
        // useless check since we fetch account with key as input..
        boolean isMatch = resetKey.equals(pass.getResetKey());

        if (!isMatch) {
            logger.error("invalid key, not matching");
            throw new InvalidDataException("key");
        }

        // validate deadline
        Calendar calendar = Calendar.getInstance();
        if (pass.getResetDeadline() == null) {
            logger.error("corrupt or used key, missing deadline");
            // do not leak reason
            throw new InvalidDataException("key");
        }

        boolean isExpired = calendar.after(pass.getResetDeadline());

        if (isExpired) {
            logger.error("expired key on " + String.valueOf(pass.getResetDeadline()));
            // do not leak reason
            throw new InvalidDataException("key");
        }

        isValid = isActive && isMatch && !isExpired;

        if (!isValid) {
            throw new InvalidDataException("key");
        }

        // map to ourselves
        pass.setProvider(getProvider());

        // password are encrypted, but clear value for extra safety
        pass.eraseCredentials();

        return pass;
    }

    //DISABLED, management should happen via credentialsService!
    // public void deletePassword(String userId, String username) throws NoSuchUserException {
    //     // TODO add locking for atomic operation
    //     logger.debug("delete all passwords for user {} username {}", String.valueOf(userId), String.valueOf(username));

    //     // fetch all to collect ids
    //     List<InternalUserPassword> passwords = passwordService
    //         .findCredentialsByUser(repositoryId, userId)
    //         .stream()
    //         .filter(p -> p.getUsername().equals(username))
    //         .collect(Collectors.toList());

    //     // delete in batch
    //     Set<String> ids = passwords.stream().map(p -> p.getId()).collect(Collectors.toSet());
    //     passwordService.deleteAllCredentials(repositoryId, ids);

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

    /*
     * Mail
     */
    private void sendResetMail(InternalUserAccount account, String key, Date expirationDate) throws MessagingException {
        if (mailService != null) {
            // action is handled by global filter
            String provider = getProvider();
            String username = account.getUsername();
            String resetUrl =
                PasswordIdentityAuthority.AUTHORITY_URL +
                "doreset/" +
                provider +
                "?username=" +
                UriUtils.encodeQueryParam(username, StandardCharsets.UTF_8) +
                "&code=" +
                key;
            if (uriBuilder != null) {
                resetUrl = uriBuilder.buildUrl(null, resetUrl);
            }
            Map<String, String> action = new HashMap<>();
            action.put("url", resetUrl);
            action.put("text", "action.reset");

            Locale locale = account.getLang() != null
                ? Locale.forLanguageTag(account.getLang())
                : LocaleContextHolder.getLocale();
            DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale);

            Map<String, String> reset = new HashMap<>();
            reset.put("username", username);
            reset.put("key", key);
            reset.put("expire", formatter.format(expirationDate));

            String realm = getRealm();
            String realmUrl = "";
            String logoUrl = "";
            if (uriBuilder != null) {
                realmUrl = uriBuilder.buildUrl(realm, "/login");
                logoUrl = uriBuilder.buildUrl(realm, "/logo");
            }

            Map<String, String> application = new HashMap<>();
            application.put("name", realm);
            application.put("url", realmUrl);
            application.put("logo", logoUrl);
            application.put("email", "");

            if (realmService != null) {
                Realm r = realmService.findRealm(realm);
                if (r != null) {
                    application.put("name", r.getName());
                    application.put("email", Optional.ofNullable(r.getEmail()).orElse(""));
                }
            }

            Map<String, Object> vars = new HashMap<>();
            vars.put("user", account);
            vars.put("action", action);
            vars.put("reset", reset);
            vars.put("realm", account.getRealm());
            vars.put("application", application);

            String template = "reset";
            mailService.sendEmail(account.getEmail(), template, account.getLang(), vars);
        }
    }
}
