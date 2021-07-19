package it.smartcommunitylab.aac.internal.provider;

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
import it.smartcommunitylab.aac.internal.service.InternalUserAccountService;
import it.smartcommunitylab.aac.model.Subject;

public class InternalSubjectResolver extends AbstractProvider
        implements SubjectResolver {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private InternalAccountProvider accountProvider;

    public InternalSubjectResolver(String providerId, InternalUserAccountService userAccountService, String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId, realm);
        Assert.notNull(userAccountService, "user account service is mandatory");

        // build an internal provider bound to repository
        this.accountProvider = new InternalAccountProvider(providerId, userAccountService, realm);
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_SUBJECT;
    }

    @Override
    public Subject resolveByUserId(String userId) {
        logger.debug("resolve by user id " + userId);
        try {
            InternalUserAccount account = accountProvider.getAccount(userId);

            // build subject with username
            return new Subject(account.getSubject(), account.getUsername());
        } catch (NoSuchUserException nex) {
            return null;
        }
    }

    @Override
    public Subject resolveByIdentifyingAttributes(Map<String, String> attributes) {
        try {
            // let provider resolve to an account
            InternalUserAccount account = accountProvider.getByIdentifyingAttributes(attributes);

            // build subject with username
            return new Subject(account.getSubject(), account.getUsername());
        } catch (NoSuchUserException nex) {
            return null;
        }
    }

//    @Override
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

        // include unique keys
        attributes.add(Collections.singleton("confirmationKey"));
        attributes.add(Collections.singleton("resetkey"));

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
                InternalUserAccount account = accountProvider.getByIdentifyingAttributes(idAttrs);

                // build subject with username
                return new Subject(account.getSubject(), account.getUsername());
            } catch (NoSuchUserException nex) {
                return null;
            }
        } else {
            return null;
        }
    }

//    @Override
    public Collection<String> getLinkingAttributes() {
        // only realm+email
        // TODO re-evaluate global linking with only email
        return Stream.of("realm", "email")
                .collect(Collectors.toSet());

    }

    @Override
    public Map<String, String> getIdentifyingAttributes(UserAuthenticatedPrincipal principal) {
        if (!(principal instanceof InternalUserAuthenticatedPrincipal)) {
            return null;
        }

        InternalUserAuthenticatedPrincipal user = (InternalUserAuthenticatedPrincipal) principal;

        Map<String, String> attributes = new HashMap<>();
        // export userId
        attributes.put("userId", user.getUserId());

        if (user.getPrincipal() != null) {
            InternalUserAccount account = user.getPrincipal();

            // export internal id
            attributes.put("id", Long.toString(account.getId()));

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
