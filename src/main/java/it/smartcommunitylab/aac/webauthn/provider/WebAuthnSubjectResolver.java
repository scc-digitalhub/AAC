package it.smartcommunitylab.aac.webauthn.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.internal.model.InternalUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredentialsRepository;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccount;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnUserAccountService;

public class WebAuthnSubjectResolver extends AbstractProvider
        implements SubjectResolver {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private WebAuthnAccountService accountProvider;

    public WebAuthnSubjectResolver(String providerId, WebAuthnUserAccountService userAccountService,
            WebAuthnCredentialsRepository webAuthnCredentialsRepository,
            WebAuthnIdentityProviderConfig providerConfig, String realm) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, providerId, realm);
        Assert.notNull(userAccountService, "user account service is mandatory");

        // build an internal provider bound to repository
        this.accountProvider = new WebAuthnAccountService(
                providerId,
                userAccountService,
                webAuthnCredentialsRepository,
                providerConfig,
                realm);
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_SUBJECT;
    }

    @Override
    public Subject resolveByUserId(String userId) {
        logger.debug("resolve by user id " + userId);
        try {
            WebAuthnUserAccount account = (WebAuthnUserAccount) accountProvider.getAccount(userId);

            // build subject with username
            return new Subject(account.getSubject(), getRealm(), account.getUsername(), SystemKeys.RESOURCE_USER);
        } catch (NoSuchUserException nex) {
            return null;
        }
    }

    @Override
    public Subject resolveByIdentifyingAttributes(Map<String, String> attributes) {
        try {
            // let provider resolve to an account
            WebAuthnUserAccount account = (WebAuthnUserAccount) accountProvider.getByIdentifyingAttributes(attributes);

            // build subject with username
            return new Subject(account.getSubject(), getRealm(), account.getUsername(), SystemKeys.RESOURCE_USER);
        } catch (NoSuchUserException nex) {
            return null;
        }
    }

    public Collection<Set<String>> getIdentifyingAttributes() {
        // hardcoded, see repository
        List<Set<String>> attributes = new ArrayList<>();
        // id is enough
        attributes.add(Collections.singleton("userId"));

        // also realm+id attributes
        // init via stream to get an immutable set
        attributes.add(Stream.of("realm", "username")
                .collect(Collectors.toSet()));
        attributes.add(Stream.of("realm", "email")
                .collect(Collectors.toSet()));

        attributes.add(Collections.singleton("userHandle"));

        return attributes;
    }

    @Override
    public Subject resolveByLinkingAttributes(Map<String, String> attributes) {

        if (attributes.keySet().containsAll(Arrays.asList("realm", "email"))
                && getRealm().equals((attributes.get("realm")))) {
            // ensure we don't use additional params
            Map<String, String> idAttrs = new HashMap<>();
            idAttrs.put("realm", getRealm());
            idAttrs.put("email", attributes.get("email"));
            // let provider resolve to an account
            try {
                WebAuthnUserAccount account = (WebAuthnUserAccount) accountProvider.getByIdentifyingAttributes(idAttrs);

                // build subject with username
                return new Subject(account.getSubject(), getRealm(), account.getUsername(), SystemKeys.RESOURCE_USER);
            } catch (NoSuchUserException nex) {
                return null;
            }
        } else {
            return null;
        }
    }

    // @Override
    public Collection<String> getLinkingAttributes() {
        // only realm+email
        // TODO re-evaluate global linking with only email
        return Stream.of("realm", "email")
                .collect(Collectors.toSet());

    }

    @Override
    public Map<String, String> getIdentifyingAttributes(UserAuthenticatedPrincipal principal) {
        if (!(principal instanceof WebAuthnUserAuthenticatedPrincipal)) {
            return null;
        }

        WebAuthnUserAuthenticatedPrincipal user = (WebAuthnUserAuthenticatedPrincipal) principal;

        Map<String, String> attributes = new HashMap<>();
        // export userId
        attributes.put("userId", user.getUserId());

        if (user.getPrincipal() != null) {
            WebAuthnUserAccount account = (WebAuthnUserAccount) user.getPrincipal();

            // export internal id
            attributes.put("id", account.getUserHandle());

        }

        return attributes;
    }

    @Override
    public Map<String, String> getLinkingAttributes(UserAuthenticatedPrincipal principal) {
        if (!(principal instanceof InternalUserAuthenticatedPrincipal)) {
            return null;
        }

        InternalUserAuthenticatedPrincipal user = (InternalUserAuthenticatedPrincipal) principal;
        Map<String, String> attributes = new HashMap<>();
        attributes.put("realm", getRealm());

        // export userId
        attributes.put("userId", user.getUserId());

        if (user.getPrincipal() != null) {
            InternalUserAccount account = user.getPrincipal();
            if (account.isConfirmed()) {
                // export email
                attributes.put("email", account.getEmail());
            }
        }

        return attributes;
    }
}
