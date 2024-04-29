/**
 * Copyright 2024 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.users.model;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.model.UserStatus;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class UserSubject implements Subject {

    @NotBlank
    private String userId;

    @NotBlank
    private String realm;

    private String userName;

    private UserStatus status;

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_USER;
    }

    @Override
    public String getId() {
        return userId;
    }

    @Override
    public String getName() {
        return userName;
    }

    @Override
    public boolean isActive() {
        return UserStatus.ACTIVE == status;
    }

    @Override
    public boolean isBlocked() {
        return UserStatus.BLOCKED == status;
    }
}
