/**
 * Copyright 2023 Fondazione Bruno Kessler
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

package it.smartcommunitylab.aac.users.persistence;

import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.users.model.UserStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Component
public class UserEntityConverter implements Converter<UserEntity, User> {

    @Override
    public User convert(UserEntity ue) {
        Assert.notNull(ue, "user entity can not be null");
        User u = new User(ue.getUuid(), ue.getRealm());
        u.setUsername(ue.getUsername());
        u.setEmail(ue.getEmailAddress());
        u.setEmailVerified(ue.isEmailVerified());

        u.setLang(ue.getLang());

        UserStatus status = StringUtils.hasText(ue.getStatus()) ? UserStatus.parse(ue.getStatus()) : UserStatus.ACTIVE;
        u.setStatus(status);

        u.setExpirationDate(ue.getExpirationDate());
        u.setCreateDate(ue.getCreateDate());
        u.setModifiedDate(ue.getModifiedDate());

        u.setLoginDate(ue.getLoginDate());
        u.setLoginIp(ue.getLoginIp());
        u.setLoginProvider(ue.getLoginProvider());

        //TODO remove
        if (ue.getTosAccepted() != null) {
            u.setTosAccepted(ue.isTosAccepted());
        } else {
            u.setTosAccepted(null);
        }

        return u;
    }
}
