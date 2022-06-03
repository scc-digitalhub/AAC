package it.smartcommunitylab.aac.core.service;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.UserTranslator;
import it.smartcommunitylab.aac.model.User;

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
    private UserTranslator translator;

    public UserTranslatorService(UserTranslator translator) {
        if (translator != null) {
            this.translator = translator;
        } else {
            // no-op translator
            this.translator = (u, r) -> (u);
        }
    }

    public void setTranslator(UserTranslator translator) {
        this.translator = translator;
    }

    public User translate(User user, String realm) {
        if (user.getRealm().equals(realm)) {
            // short circuit, nothing to do
            return user;
        }

        return translator.translate(user, realm);

    }

}
