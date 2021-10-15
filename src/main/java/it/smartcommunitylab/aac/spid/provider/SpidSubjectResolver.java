package it.smartcommunitylab.aac.spid.provider;

import java.util.ArrayList;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.spid.auth.SpidAuthenticatedPrincipal;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccount;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccountRepository;

@Transactional
public class SpidSubjectResolver extends AbstractProvider implements SubjectResolver {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SpidAccountProvider accountProvider;
    private final SpidIdentityProviderConfig providerConfig;

    protected SpidSubjectResolver(String providerId, SpidUserAccountRepository accountRepository,
            SpidIdentityProviderConfig config,
            String realm) {
        super(SystemKeys.AUTHORITY_SPID, providerId, realm);
        Assert.notNull(accountRepository, "account repository is mandatory");
        Assert.notNull(config, "provider config is mandatory");

        this.providerConfig = config;

        // build an internal provider bound to repository
        this.accountProvider = new SpidAccountProvider(providerId, accountRepository, config, realm);
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_SUBJECT;
    }

    @Override
    @Transactional(readOnly = true)
    public Subject resolveByUserId(String userId) {
        logger.debug("resolve by user id " + userId);
        try {
            SpidUserAccount account = accountProvider.getAccount(userId);

            // build subject with username
            return new Subject(account.getSubject(), getRealm(), account.getUsername(), SystemKeys.RESOURCE_USER);
        } catch (NoSuchUserException nex) {
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Subject resolveByIdentifyingAttributes(Map<String, String> attributes) {
        try {
            // let provider resolve to an account
            SpidUserAccount account = accountProvider.getByIdentifyingAttributes(attributes);

            // build subject with username
            return new Subject(account.getSubject(), getRealm(), account.getUsername(), SystemKeys.RESOURCE_USER);
        } catch (NoSuchUserException nex) {
            return null;
        }
    }

//    @Override
//    @Transactional(readOnly = true)
    public Collection<Set<String>> getIdentifyingAttributes() {
        // hardcoded, see repository
        List<Set<String>> attributes = new ArrayList<>();
        // id is enough
        attributes.add(Collections.singleton("userId"));

        // also realm+id attributes
        // init via stream to get an immutable set
        attributes.add(Stream.of("realm", "provider", "userId")
                .collect(Collectors.toSet()));
        attributes.add(Stream.of("realm", "provider", "email")
                .collect(Collectors.toSet()));

        return attributes;
    }

    @Override
    @Transactional(readOnly = true)
    public Subject resolveByLinkingAttributes(Map<String, String> attributes) {
        // not linkable by design
        return null;
    }

//    @Override
    public Collection<String> getLinkingAttributes() {
        // not linkable by design
        return null;
    }

    @Override
    public Map<String, String> getIdentifyingAttributes(UserAuthenticatedPrincipal principal) {
        if (!(principal instanceof SpidAuthenticatedPrincipal)) {
            return null;
        }

        Map<String, String> attributes = new HashMap<>();

        // export userId
        attributes.put("userId", exportInternalId(principal.getUserId()));

        return attributes;
    }

    @Override
    public Map<String, String> getLinkingAttributes(UserAuthenticatedPrincipal principal) {
        // not linkable by design
        return null;
    }

}
