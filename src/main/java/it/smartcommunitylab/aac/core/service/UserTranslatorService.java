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

import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.UserTranslator;
import it.smartcommunitylab.aac.model.User;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/*
 * This service translates a full user representations, as usable inside the source realm,
 * into one suitable for consumption under the requested realm.
 * Identities and attributes will be filtered according to a suitable policy,
 * while realm-specific content will be added (realm authorities, custom attributes etc) by another service.
 *
 * This should be consumed from a suitable user service, able to integrate the translation with additional content.
 */

@Service
public class UserTranslatorService {

    // global translator
    // TODO add per realm translators, either per source, per dest or per combo?
    private final UserTranslator translator;

    public UserTranslatorService(UserTranslator translator) {
        Assert.notNull(translator, "translator is required");
        this.translator = translator;
    }

    public User translate(UserDetails details, String realm) {
        User user = new User(details);

        // TODO
        //        user.setAuthorities();
        //        user.setRoles(roles);

        if (realm == null || details.getRealm().equals(realm)) {
            return user;
        } else {
            return translator.translate(user, realm);
        }
    }

    public User translate(User user, String realm) {
        if (user.getRealm().equals(realm)) {
            // short circuit, nothing to do
            return user;
        }

        return translator.translate(user, realm);
    }

    protected UserIdentity translateIdentity(UserIdentity identity, String realm) {
        if (identity.getRealm().equals(realm)) {
            // short circuit, nothing to do
            return identity;
        }

        return translator.translate(identity, realm);
    }

    protected UserAttributes translateIdentity(UserAttributes attributes, String realm) {
        if (attributes.getRealm().equals(realm)) {
            // short circuit, nothing to do
            return attributes;
        }

        return translator.translate(attributes, realm);
    }
}
