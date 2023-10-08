/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.core.service;

import it.smartcommunitylab.aac.attributes.model.UserAttributes;
import it.smartcommunitylab.aac.core.auth.RealmGrantedAuthority;
import it.smartcommunitylab.aac.identity.model.UserIdentity;
import it.smartcommunitylab.aac.users.UserTranslator;
import it.smartcommunitylab.aac.users.model.User;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/*
 * Core translators leaves only info matching destination realm.
 * This class should be extended to fetch destination realm properties/attributes to be used by services
 *
 */
@Deprecated
public class CoreUserTranslator implements UserTranslator {

    @Override
    public User translate(User user, String realm) {
        User result = new User(user.getUserId(), user.getRealm());
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

        // // all roles
        // result.setSpaceRoles(user.getSpaceRoles());

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
