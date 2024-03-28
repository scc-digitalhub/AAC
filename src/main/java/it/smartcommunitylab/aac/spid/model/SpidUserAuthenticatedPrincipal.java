/*
 * Copyright 2024 the original author or authors
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

package it.smartcommunitylab.aac.spid.model;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.identity.base.AbstractUserAuthenticatedPrincipal;
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

public class SpidUserAuthenticatedPrincipal extends AbstractUserAuthenticatedPrincipal {

    private static final long serialVersionUID = SystemKeys.AAC_SPID_SERIAL_VERSION;
    public static final String RESOURCE_TYPE = String.join(
        SystemKeys.ID_SEPARATOR,
        SystemKeys.RESOURCE_PRINCIPAL,
        SystemKeys.AUTHORITY_SAML
    );
    // subject identifier from external provider is local id
    private final String subjectId;
    private Saml2AuthenticatedPrincipal principal;
    private Map<String, Serializable> attributes; // locally set attributes, for example after custom mapping
    // spid upstream idp
    private String idp;
    // spidCode identifier
    private String spidCode;
    private String username;
    private String emailAddress; // NOTE: spid email is always trusted

    public SpidUserAuthenticatedPrincipal(String provider, String realm, String userId, String subjectId) {
        super(SystemKeys.AUTHORITY_SPID, provider, realm, userId, subjectId);
        Assert.notNull(subjectId, "subjectId cannot be null");
        this.subjectId = subjectId;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public Saml2AuthenticatedPrincipal getPrincipal() {
        return principal;
    }

    public String getIdp() {
        return idp;
    }

    public String getSpidCode() {
        return spidCode;
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
    public String getUsername() {
        return username;
    }

    @Override
    public String getEmailAddress() {
        return emailAddress;
    }

    @Override
    public String getType() {
        return RESOURCE_TYPE;
    }

    @Override
    public Map<String, Serializable> getAttributes() {
        Map<String, Serializable> result = new HashMap<>();
        // get allowed attributes as string or list of strings
        if (principal != null) {
            principal
                .getAttributes()
                .entrySet()
                .stream()
                .filter(e -> (e.getValue() != null))
                .forEach(e -> {
                    String key = e.getKey();
                    List<String> values = e.getValue().stream().map(Object::toString).collect(Collectors.toList());
                    if (values.size() == 1) {
                        result.put(key, values.get(0));
                    } else {
                        result.put(key, new ArrayList<>(values));
                    }
                });
        }
        // lod local attributes - they override saml attributes when set
        if (attributes != null) {
            attributes
                .entrySet()
                .stream()
                .filter(e -> !SamlKeys.SAML_ATTRIBUTES.contains(e.getKey()))
                .filter(e -> (e.getValue() != null))
                .forEach(e -> result.put(e.getKey(), e.getValue()));
        }

        // make sure these are never overridden
        result.put("provider", getProvider());
        result.put("subjectId", subjectId);
        result.put("id", subjectId);

        if (StringUtils.hasText(spidCode)) {
            result.put("spidCode", spidCode);
        }
        if (StringUtils.hasText(idp)) {
            result.put("idp", idp);
        }

        if (StringUtils.hasText(username)) {
            result.put("username", username);
        }

        if (StringUtils.hasText(emailAddress)) {
            result.put("email", emailAddress);
            // spid email is always trusted
            result.put("emailVerified", true);
        }

        return result;
    }

    @Override
    public boolean isEmailVerified() {
        return StringUtils.hasText(emailAddress);
    }

    public void setPrincipal(Saml2AuthenticatedPrincipal principal) {
        this.principal = principal;
    }

    public void setAttributes(Map<String, Serializable> attributes) {
        this.attributes = attributes;
    }

    public void setIdp(String idp) {
        this.idp = idp;
    }

    public void setSpidCode(String spidCode) {
        this.spidCode = spidCode;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }
}
