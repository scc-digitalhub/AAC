package it.smartcommunitylab.aac.saml.provider;

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
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccount;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccountRepository;

public class SamlSubjectResolver extends AbstractProvider implements SubjectResolver {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SamlAccountProvider accountProvider;

    protected SamlSubjectResolver(String providerId, SamlUserAccountRepository accountRepository, String realm) {
        super(SystemKeys.AUTHORITY_SAML, providerId, realm);
        Assert.notNull(accountRepository, "account repository is mandatory");

        // build an internal provider bound to repository
        this.accountProvider = new SamlAccountProvider(providerId, accountRepository, realm);
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_SUBJECT;
    }

    @Override
    public Subject resolveByUserId(String userId) {
        logger.debug("resolve by user id " + userId);
        try {
            SamlUserAccount account = accountProvider.getSamlAccount(userId);

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
            DefaultAccountImpl account = (DefaultAccountImpl) accountProvider.getByIdentifyingAttributes(attributes);

            // we need to fetch the internal object
            // provider has exported the internalId
            SamlUserAccount iaccount = accountProvider.getSamlAccount(account.getInternalUserId());

            // build subject with username
            return new Subject(iaccount.getSubject(), iaccount.getUsername());
        } catch (NoSuchUserException nex) {
            return null;
        }
    }

    @Override
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
    public Subject resolveByLinkingAttributes(Map<String, String> attributes) {

        if (attributes.keySet().containsAll(Arrays.asList("realm", "email"))
                && getRealm().equals((attributes.get("realm")))) {
            // ensure we don't use additional params
            Map<String, String> idAttrs = new HashMap<>();
            idAttrs.put("realm", getRealm());
            idAttrs.put("provider", getProvider());
            idAttrs.put("email", attributes.get("email"));
            // let provider resolve to an account
            try {
                DefaultAccountImpl account = (DefaultAccountImpl) accountProvider.getByIdentifyingAttributes(idAttrs);

                // we need to fetch the internal object
                // provider has exported the internalId
                SamlUserAccount iaccount = accountProvider.getSamlAccount(account.getInternalUserId());

                // build subject with username
                return new Subject(iaccount.getSubject(), iaccount.getUsername());
            } catch (NoSuchUserException nex) {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public Collection<String> getLinkingAttributes() {
        // only realm+email
        // We don't want global linking with only email
        // Security risk: if we let users link cross domain accounts, a bogus realm
        // could set a provider and obtain identities in other realms!
        return Stream.of("realm", "email")
                .collect(Collectors.toSet());

    }

}
