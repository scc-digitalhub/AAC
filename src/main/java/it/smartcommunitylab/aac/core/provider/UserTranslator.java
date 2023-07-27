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
