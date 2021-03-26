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

import java.io.Serializable;

public class BasicProfile extends AbstractProfile {

    private String name;
    private String surname;
    private String username;
    private String email;

    public BasicProfile() {
    }

    public BasicProfile(BasicProfile bp) {
        name = bp.getName();
        surname = bp.getSurname();
        username = bp.getUsername();
        email = bp.getEmail();
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
