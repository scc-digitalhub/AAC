package it.smartcommunitylab.aac.webauthn.controller;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.RealmManager;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.webauthn.WebAuthnCredentialsAuthority;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserCredential;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnCredentialsService;

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
            Model model) throws NoSuchProviderException, NoSuchRealmException, NoSuchUserException {
        // first check uuid vs user
        UserDetails user = authHelper.getUserDetails();
        if (user == null) {
            throw new InsufficientAuthenticationException("error.unauthenticated_user");
        }

        // fetch internal identities
        Set<UserIdentity> identities = user.getIdentities().stream()
                .filter(i -> (i instanceof InternalUserIdentity))
                .collect(Collectors.toSet());

        // pick matching by uuid
        UserIdentity identity = identities.stream().filter(i -> i.getAccount().getUuid().equals(uuid))
                .findFirst().orElse(null);
        if (identity == null) {
            throw new IllegalArgumentException("error.invalid_user");
        }

        logger.debug("manage credentials for {} with provider {}", StringUtils.trimAllWhitespace(uuid),
                StringUtils.trimAllWhitespace(providerId));

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
        Map<String, String> resources = new HashMap<>();
//        if (!realm.equals(SystemKeys.REALM_COMMON)) {
//            re = realmManager.getRealm(realm);
//            displayName = re.getName();
//            CustomizationBean gcb = re.getCustomization("global");
//            if (gcb != null) {
//                resources.putAll(gcb.getResources());
//            }
//            CustomizationBean rcb = re.getCustomization("registration");
//            if (rcb != null) {
//                resources.putAll(rcb.getResources());
//            }
//        }

        model.addAttribute("displayName", displayName);
        model.addAttribute("customization", resources);

        // fetch credentials
        Collection<WebAuthnUserCredential> credentials = service.listCredentials(username);
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
    @GetMapping(value = "/webauthn/credentials/{providerId}/{uuid}/{id}")
    public String deleteCredential(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String uuid,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String id)
            throws NoSuchProviderException, RegistrationException, NoSuchUserException {

        // first check uuid vs user
        UserDetails user = authHelper.getUserDetails();
        if (user == null) {
            throw new InsufficientAuthenticationException("error.unauthenticated_user");
        }

        // fetch internal identities
        Set<UserIdentity> identities = user.getIdentities().stream()
                .filter(i -> (i instanceof InternalUserIdentity))
                .collect(Collectors.toSet());

        // pick matching by username and ignore provider
        UserIdentity identity = identities.stream().filter(i -> i.getAccount().getUuid().equals(uuid))
                .findFirst().orElse(null);
        if (identity == null) {
            throw new IllegalArgumentException("error.invalid_user");
        }

        String username = identity.getAccount().getUsername();

        // fetch provider
        WebAuthnCredentialsService service = webAuthnAuthority.getProvider(providerId);

        // get
        WebAuthnUserCredential cred = service.getCredentials(username, id);
        if (cred == null) {
            throw new RegistrationException();
        }

        // check user match
        if (!username.equals(cred.getUsername())) {
            throw new IllegalArgumentException("error.invalid_user");
        }

        logger.debug("delete credential {} for {} with provider {}", StringUtils.trimAllWhitespace(id),
                StringUtils.trimAllWhitespace(uuid),
                StringUtils.trimAllWhitespace(providerId));

        // delete
        service.deleteCredentials(username, cred.getCredentialId());

        return "redirect:/webauthn/credentials/" + providerId + "/" + uuid;

    }

}
