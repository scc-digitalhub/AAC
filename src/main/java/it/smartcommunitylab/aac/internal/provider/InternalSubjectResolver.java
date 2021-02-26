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
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.base.DefaultAccountImpl;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccountRepository;
import it.smartcommunitylab.aac.model.Subject;

public class InternalSubjectResolver extends AbstractProvider implements SubjectResolver {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private InternalAccountProvider accountProvider;

    public InternalSubjectResolver(String providerId, InternalUserAccountRepository accountRepository, String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId, realm);
        Assert.notNull(accountRepository, "account repository is mandatory");

        // build an internal provider bound to repository
        this.accountProvider = new InternalAccountProvider(providerId, accountRepository, realm);
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_SUBJECT;
    }

    @Override
    public Subject resolveByUserId(String userId) throws NoSuchUserException {
        logger.debug("resolve by user id " + userId);

        InternalUserAccount account = accountProvider.getInternalAccount(userId);

        // build subject with username
        return new Subject(account.getSubject(), account.getUsername());
    }

    @Override
    public Subject resolveByIdentifyingAttributes(Map<String, String> attributes) throws NoSuchUserException {
        String realm = getRealm();

        // let provider resolve to an account
        DefaultAccountImpl account = (DefaultAccountImpl) accountProvider.getByIdentifyingAttributes(attributes);

        // we need to fetch the internal object
        // provider has exported the internalId
        InternalUserAccount iaccount = accountProvider.getInternalAccount(account.getInternalUserId());

        // build subject with username
        return new Subject(iaccount.getSubject(), iaccount.getUsername());
    }

    @Override
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
    public Subject resolveByLinkingAttributes(Map<String, String> attributes) throws NoSuchUserException {

        if (attributes.keySet().containsAll(Arrays.asList("realm", "email"))
                && getRealm().equals((attributes.get("realm")))) {
            // ensure we don't use additional params
            Map<String, String> idAttrs = new HashMap<>();
            idAttrs.put("realm", getRealm());
            idAttrs.put("email", attributes.get("email"));
            // let provider resolve to an account
            DefaultAccountImpl account = (DefaultAccountImpl) accountProvider.getByIdentifyingAttributes(idAttrs);

            // we need to fetch the internal object
            // provider has exported the internalId
            InternalUserAccount iaccount = accountProvider.getInternalAccount(account.getInternalUserId());

            // build subject with username
            return new Subject(iaccount.getSubject(), iaccount.getUsername());
        } else {
            throw new NoSuchUserException("No internal user found matching attributes");
        }
    }

    @Override
    public Collection<String> getLinkingAttributes() {
        // only realm+email
        // TODO re-evaluate global linking with only email
        return Stream.of("realm", "email")
                .collect(Collectors.toSet());

    }

}
