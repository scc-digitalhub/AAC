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

package it.smartcommunitylab.aac.events;

import it.smartcommunitylab.aac.SystemKeys;
import org.springframework.context.ApplicationEvent;

/*
 * Published after a user is deleted. The user does not exist anymore when
 * listeners receive the event, so it carries the details needed for audit:
 * the realm and the actor (user or client) which performed the delete.
 * No personal data: the user is identified by its subject id only.
 */
public class UserDeleteEvent extends ApplicationEvent {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final String userId;
    private final String realm;
    private final String actor;

    public UserDeleteEvent(String userId, String realm, String actor) {
        // source is required, a null userId is refused by the base class
        super(userId);
        this.userId = userId;
        this.realm = realm;
        this.actor = actor;
    }

    public String getUserId() {
        return userId;
    }

    public String getRealm() {
        return realm;
    }

    public String getActor() {
        return actor;
    }
}
