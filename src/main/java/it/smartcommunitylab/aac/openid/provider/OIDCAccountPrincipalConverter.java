package it.smartcommunitylab.aac.openid.provider;

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

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.OpenIdAttributesSet;
import it.smartcommunitylab.aac.attributes.mapper.OpenIdAttributesMapper;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.model.AttributeSet;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.provider.AccountPrincipalConverter;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.openid.model.OIDCUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;

@Transactional
public class OIDCAccountPrincipalConverter extends AbstractProvider<OIDCUserAccount>
        implements AccountPrincipalConverter<OIDCUserAccount> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected final UserAccountService<OIDCUserAccount> accountService;
    protected final String repositoryId;

    private boolean trustEmailAddress;
    private boolean alwaysTrustEmailAddress;

    // attributes
    private final OpenIdAttributesMapper openidMapper;

    public OIDCAccountPrincipalConverter(String providerId,
            UserAccountService<OIDCUserAccount> accountService,
            String realm) {
        this(SystemKeys.AUTHORITY_OIDC, providerId, accountService, realm);
    }

    public OIDCAccountPrincipalConverter(String authority, String providerId,
            UserAccountService<OIDCUserAccount> accountService,
            String realm) {
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

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

    @Override
    public OIDCUserAccount convertAccount(UserAuthenticatedPrincipal userPrincipal, String userId) {
        // we expect an instance of our model
        Assert.isInstanceOf(OIDCUserAuthenticatedPrincipal.class, userPrincipal,
                "principal must be an instance of oidc authenticated principal");
        OIDCUserAuthenticatedPrincipal principal = (OIDCUserAuthenticatedPrincipal) userPrincipal;

        // we use upstream subject for accounts
        String subject = principal.getSubject();
        String provider = getProvider();

        // attributes from provider
        String username = principal.getUsername();
        Map<String, Serializable> attributes = principal.getAttributes();

        // map attributes to openid set and flatten to string
        // we also clean every attribute and allow only plain text
        AttributeSet oidcAttributeSet = openidMapper.mapAttributes(attributes);
        Map<String, String> oidcAttributes = oidcAttributeSet.getAttributes()
                .stream()
                .collect(Collectors.toMap(
                        a -> a.getKey(),
                        a -> a.exportValue()));

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
        OIDCUserAccount account = new OIDCUserAccount(getAuthority());
        account.setRepositoryId(repositoryId);
        account.setProvider(getProvider());

        account.setSubject(subject);
        account.setUserId(userId);
        account.setRealm(getRealm());

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
