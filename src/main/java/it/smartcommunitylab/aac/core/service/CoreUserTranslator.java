package it.smartcommunitylab.aac.core.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import it.smartcommunitylab.aac.core.auth.RealmGrantedAuthority;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.UserTranslator;
import it.smartcommunitylab.aac.model.User;

/*
 * Core translators leaves only info matching destination realm.
 * 
 */
public class CoreUserTranslator implements UserTranslator {

    @Override
    public User translate(User user, String realm) {

        User result = new User(user.getUserId(), realm);

        // pass only entity attributes, valid in all realms
        result.setUsername(user.getUsername());
        result.setEmail(user.getEmail());
        result.setEmailVerified(user.isEmailVerified());
        result.setStatus(user.getStatus());

        // no identities or attributes translated
//        // filter identities
//        List<UserIdentity> identities = user.getIdentities().stream()
//                .map(i -> translate(i, realm))
//                .filter(i -> (i != null))
//                .collect(Collectors.toList());
//        result.setIdentities(identities);
//
//        // filter attribute sets
//        List<UserAttributes> attributes = user.getAttributes().stream()
//                .map(i -> translate(i, realm))
//                .filter(i -> (i != null))
//                .collect(Collectors.toList());
//        result.setAttributes(attributes);

        // filter authorities
        List<GrantedAuthority> authorities = user.getAuthorities().stream()
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

}
