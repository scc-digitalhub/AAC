package it.smartcommunitylab.aac.saml.provider;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.MissingDataException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.provider.AccountProvider;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.model.UserStatus;
import it.smartcommunitylab.aac.saml.model.SamlUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccount;
import it.smartcommunitylab.aac.saml.service.SamlUserAccountService;

@Transactional
public class SamlAccountProvider extends AbstractProvider
        implements AccountProvider<SamlUserAccount> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final UserAccountService<SamlUserAccount> accountService;
    private final String repositoryId;

    private final SamlIdentityProviderConfig config;

    protected SamlAccountProvider(String providerId,
            UserAccountService<SamlUserAccount> accountService,
            SamlIdentityProviderConfig config,
            String realm) {
        super(SystemKeys.AUTHORITY_SAML, providerId, realm);
        Assert.notNull(accountService, "account service is mandatory");
        Assert.notNull(config, "provider config is mandatory");

        this.config = config;
        this.accountService = accountService;

        // repositoryId is always providerId, saml isolates data per provider
        this.repositoryId = providerId;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

    @Override
    public SamlUserAccount convertAccount(UserAuthenticatedPrincipal userPrincipal, String userId) {
        // we expect an instance of our model
        Assert.isInstanceOf(SamlUserAuthenticatedPrincipal.class, userPrincipal,
                "principal must be an instance of saml authenticated principal");
        SamlUserAuthenticatedPrincipal principal = (SamlUserAuthenticatedPrincipal) userPrincipal;

        // we use upstream subject for accounts
        // TODO handle transient ids, for example with session persistence
        String subjectId = principal.getSubjectId();

        // attributes from provider
        String username = principal.getUsername();
        Map<String, Serializable> attributes = principal.getAttributes();

        // re-read attributes as-is, transform to strings
        // we also clean every attribute and allow only plain text
        // TODO evaluate using a custom mapper to given profile
        Map<String, String> samlAttributes = attributes.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> clean(e.getValue().toString())));

        String email = samlAttributes.get("email");
        username = StringUtils.hasText(samlAttributes.get("username"))
                ? samlAttributes.get("username")
                : principal.getUsername();

        // update additional attributes
        String issuer = samlAttributes.get("issuer");
        if (!StringUtils.hasText(issuer)) {
            issuer = config.getRelyingPartyRegistration().getAssertingPartyDetails().getEntityId();
        }

        String name = StringUtils.hasText(samlAttributes.get("name")) ? samlAttributes.get("name") : username;

        boolean defaultVerifiedStatus = config.getConfigMap().getTrustEmailAddress() != null
                ? config.getConfigMap().getTrustEmailAddress()
                : false;
        boolean emailVerified = StringUtils.hasText(samlAttributes.get("emailVerified"))
                ? Boolean.parseBoolean(samlAttributes.get("emailVerified"))
                : defaultVerifiedStatus;

        if (Boolean.TRUE.equals(config.getConfigMap().getAlwaysTrustEmailAddress())) {
            emailVerified = true;
        }

        // build model from scratch
        SamlUserAccount account = new SamlUserAccount(getAuthority());
        account.setProvider(repositoryId);
        account.setSubjectId(subjectId);
        account.setUserId(userId);
        account.setRealm(getRealm());

        account.setUsername(username);
        account.setIssuer(issuer);
        account.setName(name);
        account.setEmail(email);
        account.setEmailVerified(emailVerified);

        return account;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SamlUserAccount> listAccounts(String userId) {
        List<SamlUserAccount> accounts = accountService.findAccountByUser(repositoryId, userId);

        // map to our authority
        accounts.forEach(a -> a.setAuthority(getAuthority()));
        return accounts;
    }

    @Transactional(readOnly = true)
    public SamlUserAccount getAccount(String subjectId) throws NoSuchUserException {
        SamlUserAccount account = findAccountBySubjectId(subjectId);
        if (account == null) {
            throw new NoSuchUserException();
        }

        return account;
    }

    @Transactional(readOnly = true)
    public SamlUserAccount findAccount(String subjectId) {
        return findAccountBySubjectId(subjectId);
    }

    @Transactional(readOnly = true)
    public SamlUserAccount findAccountBySubjectId(String subjectId) {
        SamlUserAccount account = accountService.findAccountById(repositoryId, subjectId);
        if (account == null) {
            return null;
        }

        // map to our authority
        account.setAuthority(getAuthority());

        return account;
    }

    @Override
    @Transactional(readOnly = true)
    public SamlUserAccount findAccountByUuid(String uuid) {
        SamlUserAccount account = accountService.findAccountByUuid(repositoryId, uuid);
        if (account == null) {
            return null;
        }

        // map to our authority
        account.setAuthority(getAuthority());

        return account;
    }

    @Override
    public SamlUserAccount lockAccount(String subjectId) throws NoSuchUserException, RegistrationException {
        return updateStatus(subjectId, UserStatus.LOCKED);
    }

    @Override
    public SamlUserAccount unlockAccount(String subjectId) throws NoSuchUserException, RegistrationException {
        return updateStatus(subjectId, UserStatus.ACTIVE);
    }

    @Override
    public SamlUserAccount linkAccount(String subjectId, String userId)
            throws NoSuchUserException, RegistrationException {

        // we expect userId to be valid
        if (!StringUtils.hasText(userId)) {
            throw new MissingDataException("user");
        }

        SamlUserAccount account = findAccountBySubjectId(subjectId);
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
        account = accountService.updateAccount(repositoryId, subjectId, account);

        // map to our authority
        account.setAuthority(getAuthority());

        return account;
    }

    private SamlUserAccount updateStatus(String subjectId, UserStatus newStatus)
            throws NoSuchUserException, RegistrationException {
        SamlUserAccount account = findAccountBySubjectId(subjectId);
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
        account = accountService.updateAccount(repositoryId, subjectId, account);

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
