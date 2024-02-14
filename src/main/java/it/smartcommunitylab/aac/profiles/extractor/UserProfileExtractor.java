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

package it.smartcommunitylab.aac.profiles.extractor;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.identity.model.UserIdentity;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.profiles.model.AbstractProfile;
import java.util.Collection;

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
    public AbstractProfile extractUserProfile(UserIdentity identity) throws InvalidDefinitionException;

    /*
     * Get the profile from the default/primary identity, or from all identities
     * plus attributes We don't enforce implementations to choose an extraction
     * policy.
     *
     * This method is *required* to return a valid profile.
     */
    public AbstractProfile extractUserProfile(User user) throws InvalidDefinitionException;

    /*
     * Get a collection of profiles, where the user can be represented by more than
     * one. For example in case of multiple identities.
     */

    public Collection<? extends AbstractProfile> extractUserProfiles(User user) throws InvalidDefinitionException;
}
