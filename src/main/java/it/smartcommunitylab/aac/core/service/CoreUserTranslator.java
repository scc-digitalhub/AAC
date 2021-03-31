package it.smartcommunitylab.aac.core.service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.UserTranslator;
import it.smartcommunitylab.aac.model.User;

/*
 * Core translators leaves only info matching destination realm.
 * This class should be extended to fetch destination realm properties/attributes to be used by services
 * 
 */
public class CoreUserTranslator implements UserTranslator {

    @Override
    public User translate(User user, String realm) {

        User result = new User(user.getSubjectId(), user.getSource());
        result.setRealm(realm);

        // pass only username
        result.setUsername(user.getUsername());

        // filter identities
        List<UserIdentity> identities = user.getIdentities().stream()
                .map(i -> translate(i, realm))
                .filter(i -> (i != null))
                .collect(Collectors.toList());

        result.setIdentities(identities);

        // filter attribute sets
        List<UserAttributes> attributes = user.getAttributes().stream()
                .map(i -> translate(i, realm))
                .filter(i -> (i != null))
                .collect(Collectors.toList());

        result.setAttributes(attributes);

        // no authorities
        result.setAuthorities(Collections.emptySet());
        // no roles
        result.setRoles(Collections.emptySet());

        return result;

    }

    @Override
    public UserIdentity translate(UserIdentity identity, String realm) {
        if (identity.getRealm().equals(realm)) {
            return identity;
        }

        return null;
    }

    @Override
    public UserAttributes translate(UserAttributes attributes, String realm) {
        if (attributes.getRealm().equals(realm)) {
            return attributes;
        }

        return null;
    }

}
