package it.smartcommunitylab.aac.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.Subject;
import it.smartcommunitylab.aac.core.SubjectResolver;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccountRepository;

public class InternalUserSubjectResolver implements SubjectResolver {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private InternalUserAccountRepository userRepository;

    public InternalUserSubjectResolver(InternalUserAccountRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Subject resolveByUserId(String userId) throws NoSuchUserException {
        logger.debug("resolve by user id " + userId);

        Long id = null;
        // expected that the user name is the numerical identifier
        try {
            id = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new UsernameNotFoundException("Incorrect user id: " + userId);
        }

        InternalUserAccount account = userRepository.findByUserId(id);
        if (account == null) {
            throw new UsernameNotFoundException("Internal user with id " + id + " does not exist.");
        }

        // build subject with username
        return new Subject(account.getSubject(), account.getUsername());
    }

    @Override
    public Subject resolveByIdentifyingAttributes(Map<String, String> attributes) throws NoSuchUserException {
        // check if passed map contains at least one valid set and fetch account
        // TODO rewrite less hardcoded
        // note AVOID reflection, we want native image support
        InternalUserAccount account = null;
        if (attributes.containsKey("userId")) {
            account = userRepository.findByUserId(Long.parseLong(attributes.get("userId")));
        }

        if (account == null && attributes.keySet().containsAll(Arrays.asList("realm", "username"))) {
            account = userRepository.findByRealmAndUsername(attributes.get("userId"), attributes.get("userId"));
        }

        if (account == null && attributes.keySet().contains("confirmationKey")) {
            account = userRepository.findByConfirmationKey(attributes.get("confirmationKey"));
        }

        if (account == null && attributes.keySet().contains("resetKey")) {
            account = userRepository.findByConfirmationKey(attributes.get("resetKey"));
        }

        if (account == null) {
            throw new UsernameNotFoundException("No internal user found matching attributes");
        }

        // build subject with username
        return new Subject(account.getSubject(), account.getUsername());
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

        // include unique keys
        attributes.add(Collections.singleton("confirmationKey"));
        attributes.add(Collections.singleton("resetkey"));

        return attributes;
    }

}
