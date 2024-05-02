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

package it.smartcommunitylab.aac.oidc.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.attributes.OpenIdAttributesSet;
import it.smartcommunitylab.aac.attributes.mapper.OpenIdAttributesMapper;
import it.smartcommunitylab.aac.attributes.model.AttributeSet;
import it.smartcommunitylab.aac.base.provider.AbstractProvider;
import it.smartcommunitylab.aac.identity.provider.AccountPrincipalConverter;
import it.smartcommunitylab.aac.oidc.OIDCKeys;
import it.smartcommunitylab.aac.oidc.model.OIDCUserAccount;
import it.smartcommunitylab.aac.oidc.model.OIDCUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.users.model.UserAuthenticatedPrincipal;
import java.io.Serializable;
import java.util.Map;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Transactional
public class OIDCAccountPrincipalConverter
    extends AbstractProvider<OIDCUserAccount>
    implements AccountPrincipalConverter<OIDCUserAccount> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected final UserAccountService<OIDCUserAccount> accountService;
    protected final String repositoryId;

    private boolean trustEmailAddress;
    private boolean alwaysTrustEmailAddress;

    // attributes
    private final OpenIdAttributesMapper openidMapper;

    public OIDCAccountPrincipalConverter(
        String providerId,
        UserAccountService<OIDCUserAccount> accountService,
        String realm
    ) {
        this(SystemKeys.AUTHORITY_OIDC, providerId, accountService, realm);
    }

    public OIDCAccountPrincipalConverter(
        String authority,
        String providerId,
        UserAccountService<OIDCUserAccount> accountService,
        String realm
    ) {
        super(authority, providerId, realm);
        Assert.notNull(accountService, "account service is mandatory");

        this.accountService = accountService;

        // repositoryId is always providerId, oidc isolates data per provider
        this.repositoryId = providerId;

        // build mapper with default config for parsing attributes
        this.openidMapper = new OpenIdAttributesMapper();

        // config flags default
        this.trustEmailAddress = false;
        this.alwaysTrustEmailAddress = false;
    }

    public void setTrustEmailAddress(boolean trustEmailAddress) {
        this.trustEmailAddress = trustEmailAddress;
    }

    public void setAlwaysTrustEmailAddress(boolean alwaysTrustEmailAddress) {
        this.alwaysTrustEmailAddress = alwaysTrustEmailAddress;
    }

    // @Override
    // public String getType() {
    //     return SystemKeys.RESOURCE_ACCOUNT;
    // }

    @Override
    public OIDCUserAccount convertAccount(UserAuthenticatedPrincipal userPrincipal, String userId) {
        // we expect an instance of our model
        Assert.isInstanceOf(
            OIDCUserAuthenticatedPrincipal.class,
            userPrincipal,
            "principal must be an instance of oidc authenticated principal"
        );
        OIDCUserAuthenticatedPrincipal principal = (OIDCUserAuthenticatedPrincipal) userPrincipal;

        // we use upstream subject for accounts
        String subject = principal.getSubject();
        String provider = getProvider();

        // attributes from provider
        String username = principal.getUsername();
        //filter jwt attributes to keep only user attributes
        Map<String, Serializable> attributes = principal
            .getAttributes()
            .entrySet()
            .stream()
            .filter(e -> !OIDCKeys.JWT_ATTRIBUTES.contains(e.getKey()))
            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        // map attributes to openid set and flatten to string
        // we also clean every attribute and allow only plain text
        AttributeSet oidcAttributeSet = openidMapper.mapAttributes(attributes);
        Map<String, String> oidcAttributes = oidcAttributeSet
            .getAttributes()
            .stream()
            .collect(Collectors.toMap(a -> a.getKey(), a -> a.exportValue()));

        String email = clean(oidcAttributes.get(OpenIdAttributesSet.EMAIL));
        username = StringUtils.hasText(oidcAttributes.get(OpenIdAttributesSet.PREFERRED_USERNAME))
            ? clean(oidcAttributes.get(OpenIdAttributesSet.PREFERRED_USERNAME))
            : principal.getUsername();

        // update additional attributes
        String issuer = attributes.containsKey(IdTokenClaimNames.ISS)
            ? clean(attributes.get(IdTokenClaimNames.ISS).toString())
            : null;
        if (!StringUtils.hasText(issuer)) {
            issuer = provider;
        }

        String name = StringUtils.hasText(oidcAttributes.get(OpenIdAttributesSet.NAME))
            ? clean(oidcAttributes.get(OpenIdAttributesSet.NAME))
            : username;

        String familyName = clean(oidcAttributes.get(OpenIdAttributesSet.FAMILY_NAME));
        String givenName = clean(oidcAttributes.get(OpenIdAttributesSet.GIVEN_NAME));

        boolean defaultVerifiedStatus = trustEmailAddress;
        boolean emailVerified = StringUtils.hasText(oidcAttributes.get(OpenIdAttributesSet.EMAIL_VERIFIED))
            ? Boolean.parseBoolean(oidcAttributes.get(OpenIdAttributesSet.EMAIL_VERIFIED))
            : defaultVerifiedStatus;

        if (alwaysTrustEmailAddress) {
            emailVerified = true;
        }

        String lang = clean(oidcAttributes.get(OpenIdAttributesSet.LOCALE));
        // TODO evaluate how to handle external pictureURI
        String picture = clean(oidcAttributes.get(OpenIdAttributesSet.PICTURE));

        // build model from scratch
        // NOTE: this is detached and thus has NO id
        OIDCUserAccount account = new OIDCUserAccount(getAuthority(), getProvider(), getRealm(), null);
        account.setRepositoryId(repositoryId);
        account.setSubject(subject);
        account.setUserId(userId);

        account.setUsername(username);
        account.setIssuer(issuer);
        account.setName(name);
        account.setFamilyName(familyName);
        account.setGivenName(givenName);
        account.setEmail(email);
        account.setEmailVerified(emailVerified);
        account.setLang(lang);
        account.setPicture(picture);

        // also add all principal attributes for persistence
        account.setAttributes(attributes);

        return account;
    }

    private String clean(String input) {
        return clean(input, Safelist.none());
    }

    private String clean(String input, Safelist safe) {
        if (StringUtils.hasText(input)) {
            return Jsoup.clean(input, safe);
        }
        return null;
    }
}
