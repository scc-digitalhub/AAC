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

package it.smartcommunitylab.aac.saml.model;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractAuthenticatedPrincipal;
import it.smartcommunitylab.aac.saml.SamlKeys;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class SamlUserAuthenticatedPrincipal extends AbstractAuthenticatedPrincipal {

    private static final long serialVersionUID = SystemKeys.AAC_SAML_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_PRINCIPAL + SystemKeys.ID_SEPARATOR + SystemKeys.AUTHORITY_SAML;

    // subject identifier from external provider is local id
    private final String subjectId;

    // link attributes
    private Boolean emailVerified;

    // TODO handle serializable
    private Saml2AuthenticatedPrincipal principal;

    private String username;
    private String emailAddress;

    // locally set attributes, for example after custom mapping
    private Map<String, Serializable> attributes;

    public SamlUserAuthenticatedPrincipal(String provider, String realm, String userId, String subjectId) {
        super(SystemKeys.AUTHORITY_SAML, provider);
        Assert.notNull(subjectId, "subjectId cannot be null");
        this.subjectId = subjectId;
        setRealm(realm);
        setUserId(userId);
    }

    @Override
    public String getType() {
        return RESOURCE_TYPE;
    }

    public String getSubjectId() {
        return subjectId;
    }

    @Override
    public String getUsername() {
        return StringUtils.hasText(username) ? username : subjectId;
    }

    @Override
    public String getEmailAddress() {
        return emailAddress;
    }

    @Override
    public String getPrincipalId() {
        return subjectId;
    }

    @Override
    public String getName() {
        return getUsername();
    }

    @Override
    public boolean isEmailVerified() {
        boolean verified = emailVerified != null ? emailVerified.booleanValue() : false;
        return StringUtils.hasText(emailAddress) && verified;
    }

    @Override
    public Map<String, Serializable> getAttributes() {
        Map<String, Serializable> result = new HashMap<>();

        if (principal != null) {
            // get allowed attributes as strings or list of strings
            principal
                .getAttributes()
                .entrySet()
                .stream()
                .filter(e -> !SamlKeys.SAML_ATTRIBUTES.contains(e.getKey()))
                .filter(e -> (e.getValue() != null && !e.getValue().isEmpty()))
                .forEach(e -> {
                    String key = e.getKey();
                    // map to String
                    List<String> values = e.getValue().stream().map(o -> o.toString()).collect(Collectors.toList());
                    if (values.size() == 1) {
                        result.put(key, values.get(0));
                    } else {
                        result.put(key, new ArrayList<>(values));
                    }
                });
        }

        if (attributes != null) {
            // local attributes overwrite saml attributes when set
            attributes.entrySet().forEach(e -> result.put(e.getKey(), e.getValue()));
        }

        // override if set
        if (StringUtils.hasText(username)) {
            result.put("username", username);
        }

        if (StringUtils.hasText(emailAddress)) {
            result.put("email", emailAddress);
        }

        if (emailVerified != null) {
            result.put("emailVerified", emailVerified.booleanValue());
        }

        // add base attributes
        result.putAll(super.getAttributes());

        // make sure these are never overridden
        result.put("subjectId", subjectId);

        return result;
    }

    public Saml2AuthenticatedPrincipal getPrincipal() {
        return principal;
    }

    public void setPrincipal(Saml2AuthenticatedPrincipal principal) {
        this.principal = principal;
    }

    public void setAttributes(Map<String, Serializable> attributes) {
        this.attributes = attributes;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return emailAddress;
    }

    public void setEmail(String email) {
        this.emailAddress = email;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
}
