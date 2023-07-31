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

package it.smartcommunitylab.aac.webauthn.controller;

import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.realms.RealmManager;
import it.smartcommunitylab.aac.webauthn.WebAuthnCredentialsAuthority;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserCredential;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnCredentialsService;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping
public class WebAuthnCredentialsController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private AuthenticationHelper authHelper;

    @Autowired
    private WebAuthnCredentialsAuthority webAuthnAuthority;

    @Autowired
    private RealmManager realmManager;

    @Hidden
    @RequestMapping(value = "/webauthn/credentials/{providerId}/{uuid}", method = RequestMethod.GET)
    public String credentialsPage(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
        @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String uuid,
        Model model
    ) throws NoSuchProviderException, NoSuchRealmException, NoSuchUserException {
        // first check uuid vs user
        UserDetails user = authHelper.getUserDetails();
        if (user == null) {
            throw new InsufficientAuthenticationException("error.unauthenticated_user");
        }

        // fetch internal identities
        Set<UserIdentity> identities = user
            .getIdentities()
            .stream()
            .filter(i -> (i instanceof InternalUserIdentity))
            .collect(Collectors.toSet());

        // pick matching by uuid
        UserIdentity identity = identities
            .stream()
            .filter(i -> i.getAccount().getUuid().equals(uuid))
            .findFirst()
            .orElse(null);
        if (identity == null) {
            throw new IllegalArgumentException("error.invalid_user");
        }

        logger.debug(
            "manage credentials for {} with provider {}",
            StringUtils.trimAllWhitespace(uuid),
            StringUtils.trimAllWhitespace(providerId)
        );

        UserAccount account = identity.getAccount();

        // fetch provider
        WebAuthnCredentialsService service = webAuthnAuthority.getProvider(providerId);

        // for internal username is accountId
        String username = account.getAccountId();

        // build model for this account
        model.addAttribute("providerId", providerId);

        String realm = service.getRealm();
        model.addAttribute("realm", realm);

        Realm re = realmManager.getRealm(realm);
        String displayName = re.getName();
        model.addAttribute("displayName", displayName);

        // fetch credentials
        // NOTE: credentials are listed by user not account
        // TODO implement picker for account when more than one available
        String userId = user.getSubjectId();
        Collection<WebAuthnUserCredential> credentials = service.listCredentialsByUser(userId);
        model.addAttribute("credentials", credentials);

        // build url
        // TODO handle via urlBuilder or entryPoint
        model.addAttribute("deleteUrl", "/webauthn/credentials/" + providerId + "/" + uuid + "/");
        model.addAttribute("registrationUrl", "/webauthn/register/" + providerId + "/" + uuid);
        model.addAttribute("loginUrl", "/-/" + realm + "/login");
        model.addAttribute("accountUrl", "/account");

        // return credentials registration page
        return "webauthn/credentials";
    }

    @Hidden
    @DeleteMapping(value = "/webauthn/credentials/{providerId}/{id}")
    public void deleteCredential(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String id
    ) throws NoSuchProviderException, RegistrationException, NoSuchUserException {
        // first check user
        UserDetails user = authHelper.getUserDetails();
        if (user == null) {
            throw new InsufficientAuthenticationException("error.unauthenticated_user");
        }

        // fetch provider
        WebAuthnCredentialsService service = webAuthnAuthority.getProvider(providerId);

        try {
            // get
            WebAuthnUserCredential cred = service.getCredential(id);
            if (cred == null) {
                throw new RegistrationException();
            }

            // check user matches
            boolean matches = user
                .getIdentities()
                .stream()
                .filter(i -> (i.getAccount() instanceof InternalUserAccount))
                .map(i -> i.getAccount().getAccountId())
                .anyMatch(u -> cred.getAccountId().equals(u));
            if (!matches) {
                throw new IllegalArgumentException("invalid credential");
            }

            logger.debug(
                "delete credential {}  with provider {}",
                StringUtils.trimAllWhitespace(id),
                StringUtils.trimAllWhitespace(providerId)
            );

            // delete
            service.deleteCredential(cred.getCredentialsId());
        } catch (NoSuchCredentialException e) {
            // ignore
        }
    }
}
