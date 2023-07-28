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

package it.smartcommunitylab.aac.openid.model;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.OpenIdAttributesSet;
import it.smartcommunitylab.aac.base.AbstractAuthenticatedPrincipal;
import it.smartcommunitylab.aac.openid.OIDCKeys;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class OIDCUserAuthenticatedPrincipal extends AbstractAuthenticatedPrincipal {

    private static final long serialVersionUID = SystemKeys.AAC_OIDC_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_PRINCIPAL + SystemKeys.ID_SEPARATOR + SystemKeys.AUTHORITY_OIDC;

    // subject identifier from external provider is local id
    private final String subject;

    // link attributes
    private Boolean emailVerified;

    // TODO handle serializable
    private OAuth2User principal;

    private String username;
    private String emailAddress;

    // locally set attributes, for example after custom mapping
    private Map<String, Serializable> attributes;

    public OIDCUserAuthenticatedPrincipal(String provider, String realm, String userId, String subject) {
        this(SystemKeys.AUTHORITY_OIDC, provider, realm, userId, subject);
    }

    public OIDCUserAuthenticatedPrincipal(
        String authority,
        String provider,
        String realm,
        String userId,
        String subject
    ) {
        super(authority, provider);
        Assert.hasText(subject, "subject can not be null or empty");
        this.subject = subject;
        setRealm(realm);
        setUserId(userId);
    }

    @Override
    public String getType() {
        return RESOURCE_TYPE;
    }

    @Override
    public String getUsername() {
        return StringUtils.hasText(username) ? username : subject;
    }

    @Override
    public String getEmailAddress() {
        return emailAddress;
    }

    public String getSubject() {
        return subject;
    }

    @Override
    public String getPrincipalId() {
        return subject;
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
            // map only string attributes
            // TODO implement a mapper via script handling a json representation without
            // security related attributes
            principal
                .getAttributes()
                .entrySet()
                .stream()
                .filter(e -> !OIDCKeys.JWT_ATTRIBUTES.contains(e.getKey()))
                .filter(e -> (e.getValue() != null))
                .forEach(e -> {
                    // put if absent to pick only first value when repeated
                    // TODO handle full mapping
                    result.putIfAbsent(e.getKey(), e.getValue().toString());
                });

            if (isOidcUser()) {
                ((OidcUser) principal).getClaims()
                    .entrySet()
                    .stream()
                    .filter(e -> !OIDCKeys.JWT_ATTRIBUTES.contains(e.getKey()))
                    .filter(e -> (e.getValue() != null))
                    .forEach(e -> {
                        // put if absent to pick only first value when repeated
                        // TODO handle full mapping
                        result.putIfAbsent(e.getKey(), e.getValue().toString());
                    });
            }
        }

        if (attributes != null) {
            // local attributes overwrite oauth attributes when set
            attributes.entrySet().forEach(e -> result.put(e.getKey(), e.getValue()));
        }

        // override if set
        if (StringUtils.hasText(username)) {
            result.put("name", username);
        }

        if (StringUtils.hasText(emailAddress)) {
            result.put(OpenIdAttributesSet.EMAIL, emailAddress);
        }

        if (emailVerified != null) {
            result.put(OpenIdAttributesSet.EMAIL_VERIFIED, emailVerified.booleanValue());
        }

        // add base attributes
        result.putAll(super.getAttributes());

        // make sure these are never overridden
        result.put("sub", subject);

        return result;
    }

    public OAuth2User getPrincipal() {
        return principal;
    }

    public void setPrincipal(OAuth2User principal) {
        this.principal = principal;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public OAuth2User getOAuth2User() {
        return principal;
    }

    public boolean isOidcUser() {
        return (principal instanceof OidcUser);
    }

    public OidcUser getOidcUser() {
        if (isOidcUser()) {
            return (OidcUser) principal;
        }
        return null;
    }

    public void setAttributes(Map<String, Serializable> attributes) {
        this.attributes = attributes;
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
