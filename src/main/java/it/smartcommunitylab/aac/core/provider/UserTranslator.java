package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.model.User;

/*
 * Implementations need to provide a policy for translating a user model, as provided from the source realm,
 * to a representation suitable for consumption under the given destination realm.
 *
 * Do note that the translation can be a narrow down or an integration of new attributes.
 */
public interface UserTranslator {
    public User translate(User user, String realm);

    public UserIdentity translate(UserIdentity identity, String realm);

    public UserAttributes translate(UserAttributes attributes, String realm);
}
