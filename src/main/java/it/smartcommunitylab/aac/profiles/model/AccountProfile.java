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

import java.util.Map;

/**
 * User registration information (different account of the registered user)
 * 
 * @author raman
 *
 */
public class AccountProfile extends BasicProfile {

    // identitying core attributes
    private String authority;
    private String provider;

    // userId identities the user for the authority
    private String userId;

    // this contains additional attributes from provider
    // do not expose private data!
    private Map<String, String> attributes;

    public AccountProfile() {
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

//    /**
//     * @param user
//     * @return
//     */
//    public static AccountProfile fromUser(User user) {
//        if (user == null) {
//            return null;
//        }
//
//        AccountProfile minProfile = new AccountProfile();
//        Set<Attribute> attrs = user.getAttributeEntities();
//        if (attrs != null) {
//            for (Attribute a : attrs) {
//                String account = a.getAuthority().getName();
//                minProfile.addAttribute(account, a.getKey(), a.getValue());
//            }
//        }
//        minProfile.setUsername(user.getUsername());
//        minProfile.setName(user.getName());
//        minProfile.setSurname(user.getSurname());
//        minProfile.setUserId(user.getId().toString());
//
//        return minProfile;
//    }
}
