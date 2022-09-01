package it.smartcommunitylab.aac.openid.provider;

import java.io.Serializable;
import java.util.List;
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
import it.smartcommunitylab.aac.common.MissingDataException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.model.AttributeSet;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.provider.AccountProvider;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.model.UserStatus;
import it.smartcommunitylab.aac.openid.model.OIDCUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import it.smartcommunitylab.aac.openid.service.OIDCUserAccountService;

@Transactional
public class OIDCAccountProvider extends AbstractProvider implements AccountProvider<OIDCUserAccount> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final UserAccountService<OIDCUserAccount> accountService;
    private final String repositoryId;

    private final OIDCIdentityProviderConfig config;

    // attributes
    protected final OpenIdAttributesMapper openidMapper;

    public OIDCAccountProvider(String providerId,
            UserAccountService<OIDCUserAccount> accountService,
            OIDCIdentityProviderConfig config,
            String realm) {
        this(SystemKeys.AUTHORITY_OIDC, providerId, accountService, config, realm);
    }

    public OIDCAccountProvider(String authority, String providerId,
            UserAccountService<OIDCUserAccount> accountService,
            OIDCIdentityProviderConfig config,
            String realm) {
        super(authority, providerId, realm);
        Assert.notNull(accountService, "account service is mandatory");
        Assert.notNull(config, "provider config is mandatory");

        this.config = config;
        this.accountService = accountService;

        // repositoryId is always providerId, oidc isolates data per provider
        this.repositoryId = providerId;

        // build mapper with default config for parsing attributes
        this.openidMapper = new OpenIdAttributesMapper();
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

        boolean defaultVerifiedStatus = config.getConfigMap().getTrustEmailAddress() != null
                ? config.getConfigMap().getTrustEmailAddress()
                : false;
        boolean emailVerified = StringUtils.hasText(oidcAttributes.get(OpenIdAttributesSet.EMAIL_VERIFIED))
                ? Boolean.parseBoolean(oidcAttributes.get(OpenIdAttributesSet.EMAIL_VERIFIED))
                : defaultVerifiedStatus;

        if (Boolean.TRUE.equals(config.getConfigMap().getAlwaysTrustEmailAddress())) {
            emailVerified = true;
        }

        String lang = clean(oidcAttributes.get(OpenIdAttributesSet.LOCALE));
        // TODO evaluate how to handle external pictureURI
        String picture = clean(oidcAttributes.get(OpenIdAttributesSet.PICTURE));

        // build model from scratch
        OIDCUserAccount account = new OIDCUserAccount(getAuthority());
        account.setProvider(repositoryId);
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

        return account;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OIDCUserAccount> listAccounts(String userId) {
        List<OIDCUserAccount> accounts = accountService.findAccountByUser(repositoryId, userId);

        // map to our authority
        accounts.forEach(a -> a.setAuthority(getAuthority()));
        return accounts;
    }

    @Transactional(readOnly = true)
    public OIDCUserAccount getAccount(String subject) throws NoSuchUserException {
        OIDCUserAccount account = findAccountBySubject(subject);
        if (account == null) {
            throw new NoSuchUserException();
        }

        return account;
    }

    @Transactional(readOnly = true)
    public OIDCUserAccount findAccount(String subject) {
        return findAccountBySubject(subject);
    }

    @Transactional(readOnly = true)
    public OIDCUserAccount findAccountBySubject(String subject) {
        OIDCUserAccount account = accountService.findAccountById(repositoryId, subject);
        if (account == null) {
            return null;
        }

        // map to our authority
        account.setAuthority(getAuthority());

        return account;
    }

    @Override
    @Transactional(readOnly = true)
    public OIDCUserAccount findAccountByUuid(String uuid) {
        OIDCUserAccount account = accountService.findAccountByUuid(repositoryId, uuid);
        if (account == null) {
            return null;
        }

        // map to our authority
        account.setAuthority(getAuthority());

        return account;
    }

    @Override
    public OIDCUserAccount lockAccount(String subject) throws NoSuchUserException, RegistrationException {
        return updateStatus(subject, UserStatus.LOCKED);
    }

    @Override
    public OIDCUserAccount unlockAccount(String subject) throws NoSuchUserException, RegistrationException {
        return updateStatus(subject, UserStatus.ACTIVE);
    }

    @Override
    public OIDCUserAccount linkAccount(String subject, String userId)
            throws NoSuchUserException, RegistrationException {

        // we expect user to be valid
        if (!StringUtils.hasText(userId)) {
            throw new MissingDataException("user");
        }

        OIDCUserAccount account = findAccountBySubject(subject);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // check if active, inactive accounts can not be changed except for activation
        UserStatus curStatus = UserStatus.parse(account.getStatus());
        if (UserStatus.INACTIVE == curStatus) {
            throw new IllegalArgumentException("account is inactive, activate first to update status");
        }

        // re-link to user
        account.setUserId(userId);
        account = accountService.updateAccount(repositoryId, subject, account);

        // map to our authority
        account.setAuthority(getAuthority());

        return account;
    }

    private OIDCUserAccount updateStatus(String subject, UserStatus newStatus)
            throws NoSuchUserException, RegistrationException {

        OIDCUserAccount account = findAccountBySubject(subject);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // check if active, inactive accounts can not be changed except for activation
        UserStatus curStatus = UserStatus.parse(account.getStatus());
        if (UserStatus.INACTIVE == curStatus && UserStatus.ACTIVE != newStatus) {
            throw new IllegalArgumentException("account is inactive, activate first to update status");
        }

        // update status
        account.setStatus(newStatus.getValue());
        account = accountService.updateAccount(repositoryId, subject, account);

        // map to our authority
        account.setAuthority(getAuthority());

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
