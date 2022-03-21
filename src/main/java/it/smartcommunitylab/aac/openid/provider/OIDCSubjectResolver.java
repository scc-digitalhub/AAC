package it.smartcommunitylab.aac.openid.provider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.openid.model.OIDCUserAttribute;
import it.smartcommunitylab.aac.openid.model.OIDCUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccountId;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccountRepository;

@Transactional
public class OIDCSubjectResolver extends AbstractProvider implements SubjectResolver {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final OIDCUserAccountRepository accountRepository;
    private final OIDCIdentityProviderConfig config;

    public OIDCSubjectResolver(String providerId, OIDCUserAccountRepository accountRepository,
            OIDCIdentityProviderConfig config,
            String realm) {
        this(SystemKeys.AUTHORITY_OIDC, providerId, accountRepository, config, realm);
    }

    public OIDCSubjectResolver(String authority, String providerId, OIDCUserAccountRepository accountRepository,
            OIDCIdentityProviderConfig config,
            String realm) {
        super(authority, providerId, realm);
        Assert.notNull(accountRepository, "account repository is mandatory");
        Assert.notNull(config, "provider config is mandatory");

        this.accountRepository = accountRepository;
        this.config = config;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_SUBJECT;
    }

    @Transactional(readOnly = true)
    public Subject resolveBySubject(String sub) {
        logger.debug("resolve by sub " + sub);
        OIDCUserAccount account = accountRepository.findOne(new OIDCUserAccountId(getProvider(), sub));
        if (account == null) {
            return null;
        }

        // build subject with username
        return new Subject(account.getUserId(), getRealm(), account.getUsername(), SystemKeys.RESOURCE_USER);
    }

    @Override
    public Subject resolveByAccountId(String accountId) {
        // accountId is sub
        return resolveBySubject(accountId);
    }

    @Override
    public Subject resolveByPrincipalId(String principalId) {
        // principalId is sub
        return resolveBySubject(principalId);
    }

    @Override
    public Subject resolveByIdentityId(String identityId) {
        // identityId is sub
        return resolveBySubject(identityId);
    }

    @Override
    @Transactional(readOnly = true)
    public Subject resolveByAttributes(Map<String, String> attributes) {
        String attributeName = config.getIdAttribute().getValue();

        if (attributes.keySet().contains(attributeName)
                && getRealm().equals((attributes.get("realm")))) {
            // resolve to an account by attribute
            OIDCUserAccount account = null;

            if (OIDCUserAttribute.EMAIL == config.getIdAttribute()) {
                String email = attributes.get("email");
                account = accountRepository.findByProviderAndEmail(getProvider(), email).stream().findFirst()
                        .orElse(null);
            } else if (OIDCUserAttribute.USERNAME == config.getIdAttribute()) {
                String username = attributes.get(attributeName);
                account = accountRepository.findByProviderAndUsername(getProvider(), username).stream().findFirst()
                        .orElse(null);
            }

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
//    @Transactional(readOnly = true)
//    public Collection<Set<String>> getIdentifyingAttributes() {
//        // hardcoded, see repository
//        List<Set<String>> attributes = new ArrayList<>();
//        // id is enough
//        attributes.add(Collections.singleton("userId"));
//
//        // also realm+id attributes
//        // init via stream to get an immutable set
//        attributes.add(Stream.of("realm", "provider", "userId")
//                .collect(Collectors.toSet()));
//        attributes.add(Stream.of("realm", "provider", "email")
//                .collect(Collectors.toSet()));
//
//        return attributes;
//    }

//    @Override
//    @Transactional(readOnly = true)
//    public Subject resolveByLinkingAttributes(Map<String, String> attributes) {
//
//        if (!providerConfig.isLinkable()) {
//            return null;
//        }
//
//        if (attributes.keySet().containsAll(Arrays.asList("realm", "email"))
//                && getRealm().equals((attributes.get("realm")))) {
//            // ensure we don't use additional params
//            Map<String, String> idAttrs = new HashMap<>();
//            idAttrs.put("realm", getRealm());
//            idAttrs.put("provider", getProvider());
//            idAttrs.put("email", attributes.get("email"));
//            // let provider resolve to an account
//            try {
//                OIDCUserAccount account = accountProvider.getByIdentifyingAttributes(idAttrs);
//
//                // build subject with username
//                return new Subject(account.getSubject(), getRealm(), account.getUsername(), SystemKeys.RESOURCE_USER);
//            } catch (NoSuchUserException nex) {
//                return null;
//            }
//        } else {
//            return null;
//        }
//    }

//    @Override
//    public Collection<String> getLinkingAttributes() {
//        if (!providerConfig.isLinkable()) {
//            return null;
//        }
//
//        // only realm+email
//        // We don't want global linking with only email
//        // Security risk: if we let users link cross domain accounts, a bogus realm
//        // could set a provider and obtain identities in other realms!
//        return Stream.of("realm", "email")
//                .collect(Collectors.toSet());
//
//    }

    @Override
    public Map<String, String> getAttributes(UserAuthenticatedPrincipal principal) {
        if (!config.isLinkable()) {
            return null;
        }

        if (!(principal instanceof OIDCUserAuthenticatedPrincipal)) {
            return null;
        }

        OIDCUserAuthenticatedPrincipal user = (OIDCUserAuthenticatedPrincipal) principal;
        Map<String, String> attributes = new HashMap<>();
        attributes.put("realm", getRealm());
        // export userId
        attributes.put("userId", user.getUserId());

        if (OIDCUserAttribute.EMAIL == config.getIdAttribute() && user.isEmailVerified()) {
            // export email
            attributes.put("email", user.getEmail());
        }

        if (OIDCUserAttribute.USERNAME == config.getIdAttribute() && StringUtils.hasText(user.getUsername())) {
            // export username
            attributes.put("username", user.getName());
        }

        return attributes;
    }

//    @Override
//    public Map<String, String> getIdentifyingAttributes(UserAuthenticatedPrincipal principal) {
//        if (!(principal instanceof OIDCUserAuthenticatedPrincipal)) {
//            return null;
//        }
//
//        Map<String, String> attributes = new HashMap<>();
//
//        // export userId
//        attributes.put("userId", exportInternalId(principal.getUserId()));
//
//        return attributes;
//    }
//
//    @Override
//    public Map<String, String> getLinkingAttributes(UserAuthenticatedPrincipal principal) {
//        if (!providerConfig.isLinkable()) {
//            return null;
//        }
//
//        if (!(principal instanceof OIDCUserAuthenticatedPrincipal)) {
//            return null;
//        }
//
//        Map<String, String> attributes = new HashMap<>();
//        attributes.put("realm", getRealm());
//
//        // export userId
//        attributes.put("userId", exportInternalId(principal.getUserId()));
//
//        Map<String, String> map = principal.getAttributes();
//        String email = map.get("email");
//        if (StringUtils.hasText(email)) {
//            attributes.put("email", email);
//        }
//
//        return attributes;
//    }

}
