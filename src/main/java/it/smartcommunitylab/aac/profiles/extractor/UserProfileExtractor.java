package it.smartcommunitylab.aac.profiles.extractor;

import java.util.Collection;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.profiles.model.AbstractProfile;

/*
 * Profile extractors are converters which take a user and build a profile according to a given schema
 * 
 * Core implementations leverage a pre-made schema but implementations can extend the model.
 * Do note that not all method are required, implementations could choose to return one or more.
 */
public interface UserProfileExtractor {

    /*
     * Profile identifier, assumed to be also a scope in the form
     * profile.<identifier>.me
     */

    public String getIdentifier();

    /*
     * Get the profile from the given identity, where possible
     */
    public AbstractProfile extractUserProfile(UserIdentity identity)
            throws InvalidDefinitionException;

    /*
     * Get the profile from the default/primary identity, or from all identities
     * plus attributes We don't enforce implementations to choose an extraction
     * policy.
     * 
     * This method is *required* to return a valid profile.
     */
    public AbstractProfile extractUserProfile(User user)
            throws InvalidDefinitionException;

    /*
     * Get a collection of profiles, where the user can be represented by more than
     * one. For example in case of multiple identities.
     */

    public Collection<? extends AbstractProfile> extractUserProfiles(User user)
            throws InvalidDefinitionException;
}
