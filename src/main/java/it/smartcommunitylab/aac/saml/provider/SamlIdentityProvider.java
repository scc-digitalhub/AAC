package it.smartcommunitylab.aac.saml.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.store.AttributeStore;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.base.ConfigurableProperties;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.AccountProvider;
import it.smartcommunitylab.aac.core.provider.AccountService;
import it.smartcommunitylab.aac.core.provider.AttributeProvider;
import it.smartcommunitylab.aac.core.provider.CredentialsService;
import it.smartcommunitylab.aac.core.provider.IdentityService;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.saml.SamlIdentityAuthority;
import it.smartcommunitylab.aac.saml.SamlUserIdentity;
import it.smartcommunitylab.aac.saml.auth.SamlAuthenticatedPrincipal;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccount;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccountRepository;

public class SamlIdentityProvider extends AbstractProvider implements IdentityService {
    // services
    private final SamlUserAccountRepository accountRepository;
    private final AttributeStore attributeStore;

    private final SamlIdentityProviderConfig providerConfig;

    // internal providers
    private final SamlAccountProvider accountProvider;
    private final SamlAttributeProvider attributeProvider;
    private final SamlAuthenticationProvider authenticationProvider;
    private final SamlSubjectResolver subjectResolver;

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
    }

    public SamlIdentityProvider(
            String providerId,
            SamlUserAccountRepository accountRepository, AttributeStore attributeStore,
            SamlIdentityProviderConfig config,
            String realm) {
        super(SystemKeys.AUTHORITY_SAML, providerId, realm);
        Assert.notNull(accountRepository, "account repository is mandatory");
        Assert.notNull(config, "provider config is mandatory");

        // internal data repositories
        this.accountRepository = accountRepository;
        this.attributeStore = attributeStore;

        // translate configuration
        // check configuration
        Assert.isTrue(providerId.equals(config.getProvider()),
                "configuration does not match this provider");
        Assert.isTrue(realm.equals(config.getRealm()), "configuration does not match this provider");
        this.providerConfig = config;

        // build resource providers, we use our providerId to ensure consistency
        this.accountProvider = new SamlAccountProvider(providerId, accountRepository, realm);
        this.attributeProvider = new SamlAttributeProvider(providerId, accountRepository, attributeStore, realm);
        this.authenticationProvider = new SamlAuthenticationProvider(providerId, accountRepository, realm);
        this.subjectResolver = new SamlSubjectResolver(providerId, accountRepository, realm);

    }

    @Override
    public ExtendedAuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    @Override
    public AccountProvider getAccountProvider() {
        return accountProvider;
    }

    @Override
    public AttributeProvider getAttributeProvider() {
        return attributeProvider;
    }

    @Override
    public SubjectResolver getSubjectResolver() {
        return subjectResolver;
    }

    @Override
    @Transactional(readOnly = false)
    public SamlUserIdentity convertIdentity(UserAuthenticatedPrincipal principal, String subjectId)
            throws NoSuchUserException {
        // we expect an instance of our model
        SamlAuthenticatedPrincipal user = (SamlAuthenticatedPrincipal) principal;
        // we use internal id for accounts
        String userId = parseResourceId(user.getUserId());
        String realm = getRealm();
        String provider = getProvider();
        Map<String, String> attributes = user.getAttributes();

        if (subjectId == null) {
            // this better exists
            throw new NoSuchUserException();

        }

        // TODO handle not persisted configuration
        //
        // look in repo or create
        SamlUserAccount account = accountRepository.findByRealmAndProviderAndUserId(realm, provider, userId);

        if (account == null) {

            account = new SamlUserAccount();
            account.setSubject(subjectId);
            account.setUserId(userId);
            account.setProvider(provider);
            account.setRealm(realm);
            account = accountRepository.saveAndFlush(account);
        } else {
            // force link
            // TODO re-evaluate
            account.setSubject(subjectId);

        }

        // update base attributes
        // TODO
        String issuer = attributes.get(IdTokenClaimNames.ISS);
        if (!StringUtils.hasText(issuer)) {
            issuer = provider;
        }
        account.setIssuer(issuer);

        // TODO export static map for names
        String username = user.getName();
        String name = attributes.get("name");
        String email = attributes.get("email");
        String lang = attributes.get("locale");

        // we override every time
        account.setUsername(username);
        account.setName(name);
        account.setEmail(email);
        account.setLang(lang);

        account = accountRepository.saveAndFlush(account);

        // TODO add additional attributes

        // build identity
        // detach account
        account = accountRepository.detach(account);

        // export userId
        account.setUserId(exportInternalId(userId));

        // write custom model
        SamlUserIdentity identity = SamlUserIdentity.from(account);
        return identity;
    }

    @Override
    @Transactional(readOnly = true)
    public SamlUserIdentity getIdentity(String subject, String userId) throws NoSuchUserException {
        SamlUserAccount account = accountProvider.getAccount(userId);

        if (!account.getSubject().equals(subject)) {
            throw new NoSuchUserException();
        }

        // write custom model
        SamlUserIdentity identity = SamlUserIdentity.from(account);
        return identity;

    }

    @Override
    @Transactional(readOnly = true)
    public SamlUserIdentity getIdentity(String subject, String userId, boolean fetchAttributes)
            throws NoSuchUserException {
        // TODO add attributes load
        return getIdentity(subject, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<UserIdentity> listIdentities(String subject) {
        // TODO handle not persisted configuration
        List<UserIdentity> identities = new ArrayList<>();

        Collection<SamlUserAccount> accounts = accountProvider.listAccounts(subject);

        for (SamlUserAccount account : accounts) {
            // write custom model
            SamlUserIdentity identity = SamlUserIdentity.from(account);

            identities.add(identity);
        }

        return identities;

    }

    @Override
    public String getAuthenticationUrl() {
        // TODO build a realm-bound url, need updates on filters
        return SamlIdentityAuthority.AUTHORITY_URL
                + "authenticate/" + getProvider();
    }

    @Override
    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        // we don't have one
        return null;
    }

    @Override
    public boolean canRegister() {
        return false;
    }

    @Override
    public boolean canUpdate() {
        return false;
    }

    @Override
    public boolean canDelete() {
        return true;
    }

    @Override
    public AccountService getAccountService() {
        // TODO implement a delete-only accountService
        return null;
    }

    @Override
    public CredentialsService getCredentialsService() {
        // nothing to handle
        return null;
    }

    @Override
    @Transactional(readOnly = false)
    public UserIdentity registerIdentity(
            String subject, UserAccount account,
            Collection<UserAttributes> attributes)
            throws NoSuchUserException, RegistrationException {
        throw new RegistrationException("registration not supported");
    }

    @Override
    @Transactional(readOnly = false)
    public UserIdentity updateIdentity(String subject,
            String userId, UserAccount account,
            Collection<UserAttributes> attributes)
            throws NoSuchUserException, RegistrationException {
        throw new RegistrationException("update not supported");

    }

    @Override
    @Transactional(readOnly = false)
    public void deleteIdentity(String subjectId, String userId) throws NoSuchUserException {
        // TODO delete via service

    }

    @Override
    @Transactional(readOnly = false)
    public void deleteIdentities(String subjectId) {
        // TODO Auto-generated method stub

    }

    @Override
    public String getRegistrationUrl() {
        return null;
    }

    @Override
    public String getName() {
        return providerConfig.getName();
    }

    @Override
    public String getDescription() {
        return providerConfig.getDescription();
    }

    @Override
    public ConfigurableProperties getConfiguration() {
        return providerConfig;
    }
}
