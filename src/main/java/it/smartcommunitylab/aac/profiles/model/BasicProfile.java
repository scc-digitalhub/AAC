/*******************************************************************************
 * Copyright 2015-2019 Smart Community Lab, FBK
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/
package it.smartcommunitylab.aac.profiles.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractProfile;

@JsonInclude(Include.NON_EMPTY)
public class BasicProfile extends AbstractProfile {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    public static final String IDENTIFIER = "basicprofile";

    private String name;
    private String surname;
    private String username;
    private String email;

    public BasicProfile(String authority, String provider, String realm, String userId) {
        super(authority, provider, realm, userId);
    }

    @Override
    public String getId() {
        return IDENTIFIER;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

//    /**
//     * @param user
//     * @return
//     */
//    public static BasicProfile fromUser(User user) {
//        if (user == null) {
//            return null;
//        }
//        BasicProfile profile = new BasicProfile();
//        profile.setName(user.getName());
//        profile.setSurname(user.getSurname());
//        profile.setUserId(user.getId().toString());
//        profile.setUsername(user.getUsername());
//        return profile;
//    }

}
