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

package it.smartcommunitylab.aac.internal.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.identity.base.AbstractUserAuthenticatedPrincipal;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalUserAuthenticatedPrincipal extends AbstractUserAuthenticatedPrincipal {

    private static final long serialVersionUID = SystemKeys.AAC_INTERNAL_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_PRINCIPAL + SystemKeys.ID_SEPARATOR + SystemKeys.AUTHORITY_INTERNAL;

    private final String username;

    private String emailAddress;
    private String name;
    private Boolean confirmed;

    // internal attributes from account
    private Map<String, String> attributes;

    public InternalUserAuthenticatedPrincipal(String provider, String realm, String userId, String username) {
        this(SystemKeys.AUTHORITY_INTERNAL, provider, realm, userId, username);
    }

    public InternalUserAuthenticatedPrincipal(
        String authority,
        String provider,
        String realm,
        String userId,
        String username
    ) {
        super(authority, provider, realm, username, userId);
        Assert.hasText(username, "username can not be null or empty");
        this.username = username;
    }

    @Override
    public String getType() {
        return RESOURCE_TYPE;
    }

    @Override
    public String getPrincipalId() {
        return username;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getEmailAddress() {
        return emailAddress;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isEmailVerified() {
        return isEmailConfirmed();
    }

    @Override
    public Map<String, Serializable> getAttributes() {
        Map<String, Serializable> map = new HashMap<>();

        // add all account attributes if set
        if (attributes != null) {
            map.putAll(attributes);
        }

        // override if set
        if (StringUtils.hasText(name)) {
            map.put("name", name);
        }

        if (StringUtils.hasText(emailAddress)) {
            map.put("email", emailAddress);
        }
        if (confirmed != null) {
            map.put("confirmed", Boolean.toString(confirmed.booleanValue()));
        }

        // add base attributes
        map.putAll(super.getAttributes());

        // make sure these are never overridden
        map.put("username", username);

        return map;
    }

    public String getEmail() {
        return emailAddress;
    }

    public void setEmail(String email) {
        this.emailAddress = email;
    }

    public boolean isEmailConfirmed() {
        boolean verified = confirmed != null ? confirmed.booleanValue() : false;
        return StringUtils.hasText(emailAddress) && verified;
    }

    public Boolean getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(Boolean confirmed) {
        this.confirmed = confirmed;
    }

    public void setAccountAttributes(InternalUserAccount account) {
        if (account != null) {
            this.emailAddress = account.getEmail();
            this.confirmed = account.isConfirmed();

            // map base attributes, these will be available for custom mapping
            attributes = new HashMap<>();

            String pname = account.getName();
            if (StringUtils.hasText(pname)) {
                attributes.put("name", pname);
            }

            String surname = account.getSurname();
            if (StringUtils.hasText(surname)) {
                attributes.put("surname", surname);
            }

            String lang = account.getLang();
            if (StringUtils.hasText(lang)) {
                attributes.put("lang", lang);
            }
        }
    }

    public void setName(String name) {
        this.name = name;
    }
}
