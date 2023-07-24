package it.smartcommunitylab.aac.core.service;

import it.smartcommunitylab.aac.core.auth.RealmGrantedAuthority;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.UserTranslator;
import it.smartcommunitylab.aac.model.User;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

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

        // pass only username+email
        result.setUsername(user.getUsername());
        result.setEmail(user.getEmail());
        result.setEmailVerified(user.isEmailVerified());

        // filter identities
        List<UserIdentity> identities = user
            .getIdentities()
            .stream()
            .map(i -> translate(i, realm))
            .filter(i -> (i != null))
            .collect(Collectors.toList());

        result.setIdentities(identities);

        // filter attribute sets
        List<UserAttributes> attributes = user
            .getAttributes()
            .stream()
            .map(i -> translate(i, realm))
            .filter(i -> (i != null))
            .collect(Collectors.toList());

        result.setAttributes(attributes);

        // filter authorities
        List<GrantedAuthority> authorities = user
            .getAuthorities()
            .stream()
            .filter(a -> {
                if (a instanceof SimpleGrantedAuthority) {
                    return true;
                }
                if (a instanceof RealmGrantedAuthority) {
                    return ((RealmGrantedAuthority) a).getRealm().equals(realm);
                }

                return false;
            })
            .collect(Collectors.toList());
        result.setAuthorities(authorities);

        // all roles
        result.setSpaceRoles(user.getSpaceRoles());

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
