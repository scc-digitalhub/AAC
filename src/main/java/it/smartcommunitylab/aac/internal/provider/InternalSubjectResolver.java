package it.smartcommunitylab.aac.internal.provider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.internal.model.InternalUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.service.InternalUserAccountService;
import it.smartcommunitylab.aac.model.Subject;

public class InternalSubjectResolver extends AbstractProvider
        implements SubjectResolver {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public final static String[] ATTRIBUTES = { "email" };

    private final InternalUserAccountService accountService;
    private final InternalIdentityProviderConfig config;

    public InternalSubjectResolver(String providerId, InternalUserAccountService userAccountService,
            InternalIdentityProviderConfig providerConfig, String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId, realm);
        Assert.notNull(userAccountService, "user account service is mandatory");
        this.accountService = userAccountService;
        this.config = providerConfig;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_SUBJECT;
    }

    public Subject resolveByUsername(String username) {
        logger.debug("resolve by username " + username);
        InternalUserAccount account = accountService.findAccountByUsername(getProvider(), username);
        if (account == null) {
            return null;
        }
        // build subject with username
        return new Subject(account.getUserId(), getRealm(), account.getUsername(), SystemKeys.RESOURCE_USER);
    }

    @Override
    public Subject resolveByAccountId(String username) {
        // accountId is username
        return resolveByUsername(username);
    }

    @Override
    public Subject resolveByPrincipalId(String username) {
        // principalId is username
        return resolveByUsername(username);
    }

    @Override
    public Subject resolveByIdentityId(String username) {
        // identityId is username
        return resolveByUsername(username);
    }

//    @Override
//    public Subject resolveByIdentifyingAttributes(Map<String, String> attributes) {
//        try {
//            // let provider resolve to an account
//            InternalUserAccount account = accountProvider.getByIdentifyingAttributes(attributes);
//
//            // build subject with username
//            return new Subject(account.getSubject(), getRealm(), account.getUsername(), SystemKeys.RESOURCE_USER);
//        } catch (NoSuchUserException nex) {
//            return null;
//        }
//    }

//    @Override
//    public Collection<Set<String>> getIdentifyingAttributes() {
//        // hardcoded, see repository
//        List<Set<String>> attributes = new ArrayList<>();
//        // id is enough
//        attributes.add(Collections.singleton("userId"));
//
//        // also realm+id attributes
//        // init via stream to get an immutable set
//        attributes.add(Stream.of("realm", "username")
//                .collect(Collectors.toSet()));
//        attributes.add(Stream.of("realm", "email")
//                .collect(Collectors.toSet()));
//
//        // include unique keys
//        attributes.add(Collections.singleton("confirmationKey"));
//        attributes.add(Collections.singleton("resetkey"));
//
//        return attributes;
//    }

    @Override
    public Subject resolveByAttributes(Map<String, String> attributes) {

        if (attributes.keySet().containsAll(Arrays.asList(ATTRIBUTES))
                && getRealm().equals((attributes.get("realm")))) {
            // let provider resolve to an account
            String email = attributes.get("email");
            InternalUserAccount account = accountService.findAccountByEmail(getProvider(), email).stream().findFirst()
                    .orElse(null);

            if (account == null) {
                return null;
            }

            // build subject with username
            return new Subject(account.getUserId(), getRealm(), account.getUsername(), SystemKeys.RESOURCE_USER);
        } else {
            return null;
        }
    }

//    @Override
//    public Collection<String> getAttributes() {
//        // only realm+email
//        // TODO re-evaluate global linking with only email
//        return Stream.of("realm", "email")
//                .collect(Collectors.toSet());
//
//    }

//    @Override
//    public Map<String, String> getIdentifyingAttributes(UserAuthenticatedPrincipal principal) {
//        if (!(principal instanceof InternalUserAuthenticatedPrincipal)) {
//            return null;
//        }
//
//        InternalUserAuthenticatedPrincipal user = (InternalUserAuthenticatedPrincipal) principal;
//
//        Map<String, String> attributes = new HashMap<>();
//        // export userId
//        attributes.put("userId", user.getUserId());
//
//        if (user.getPrincipal() != null) {
//            InternalUserAccount account = user.getPrincipal();
//
//            // export internal id
//            attributes.put("id", Long.toString(account.getId()));
//
//        }
//
//        return attributes;
//    }

    @Override
    public Map<String, String> getAttributes(UserAuthenticatedPrincipal principal) {
        if (!config.isLinkable()) {
            return null;
        }

        if (!(principal instanceof InternalUserAuthenticatedPrincipal)) {
            return null;
        }

        InternalUserAuthenticatedPrincipal user = (InternalUserAuthenticatedPrincipal) principal;
        Map<String, String> attributes = new HashMap<>();
        attributes.put("realm", getRealm());
        // export userId
        attributes.put("userId", user.getUserId());

        if (user.isEmailConfirmed()) {
            // export email
            attributes.put("email", user.getEmail());
        }

        return attributes;
    }

}
