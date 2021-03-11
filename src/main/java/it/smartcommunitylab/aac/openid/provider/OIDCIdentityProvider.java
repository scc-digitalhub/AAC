package it.smartcommunitylab.aac.openid.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.AttributeStore;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.base.ConfigurableProvider;
import it.smartcommunitylab.aac.core.base.DefaultIdentityImpl;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.AccountProvider;
import it.smartcommunitylab.aac.core.provider.AttributeProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.openid.OIDCAuthenticatedPrincipal;
import it.smartcommunitylab.aac.openid.OIDCIdentityProviderConfig;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccountRepository;

public class OIDCIdentityProvider extends AbstractProvider implements IdentityProvider {

    // services

    private final OIDCUserAccountRepository accountRepository;
    private final AttributeStore attributeStore;

    private final OIDCIdentityProviderConfig providerConfig;
    private final ClientRegistration clientRegistration;

    // internal providers
    private final OIDCAccountProvider accountProvider;
    private final OIDCAttributeProvider attributeProvider;
    private final OIDCAuthenticationProvider authenticationProvider;
    private final OIDCSubjectResolver subjectResolver;

    public OIDCIdentityProvider(
            String providerId,
            OIDCUserAccountRepository accountRepository, AttributeStore attributeStore,
            ConfigurableProvider configurableProvider,
            String realm) {
        super(SystemKeys.AUTHORITY_OIDC, providerId, realm);
        Assert.notNull(accountRepository, "account repository is mandatory");
        Assert.notNull(configurableProvider, "configuration is mandatory");

        // internal data repositories
        this.accountRepository = accountRepository;
        this.attributeStore = attributeStore;

        // translate configuration
        Assert.isTrue(SystemKeys.AUTHORITY_OIDC.equals(configurableProvider.getAuthority()),
                "configuration does not match this provider");
        Assert.isTrue(providerId.equals(configurableProvider.getProvider()),
                "configuration does not match this provider");
        Assert.isTrue(realm.equals(configurableProvider.getRealm()), "configuration does not match this provider");

        providerConfig = OIDCIdentityProviderConfig.fromConfigurableProvider(configurableProvider);
        clientRegistration = providerConfig.toClientRegistration();

        // build oauth services to be used exclusively here

        // build resource providers, we use our providerId to ensure consistency
        this.accountProvider = new OIDCAccountProvider(providerId, accountRepository, realm);
        this.attributeProvider = new OIDCAttributeProvider(providerId, accountRepository, attributeStore, realm);
        this.authenticationProvider = new OIDCAuthenticationProvider(providerId, accountRepository, realm);
        this.subjectResolver = new OIDCSubjectResolver(providerId, accountRepository, realm);

    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
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

    public ClientRegistration getClientRegistration() {
        return clientRegistration;
    }

    @Override
    public UserIdentity convertIdentity(UserAuthenticatedPrincipal principal, String subjectId)
            throws NoSuchUserException {
        // we expect an instance of our model
        OIDCAuthenticatedPrincipal user = (OIDCAuthenticatedPrincipal) principal;
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
        OIDCUserAccount account = accountRepository.findByRealmAndProviderAndUserId(realm, provider, userId);

        if (account == null) {

            account = new OIDCUserAccount();
            account.setSubject(subjectId);
            account.setUserId(userId);
            account.setProvider(provider);
            account.setRealm(realm);
            account = accountRepository.save(account);
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
        String familyName = attributes.get("family_name");
        String givenName = attributes.get("given_name");
        String email = attributes.get("email");
        boolean emailVerified = StringUtils.hasText(attributes.get("email_verified"))
                ? Boolean.parseBoolean(attributes.get("email_verified"))
                : false;

        String picture = attributes.get("picture");
        String lang = attributes.get("locale");

        // we override every time
        account.setUsername(username);
        account.setName(name);
        account.setFamilyName(familyName);
        account.setGivenName(givenName);
        account.setEmail(email);
        account.setEmailVerified(emailVerified);
        account.setPictureUri(picture);
        account.setLang(lang);

        account = accountRepository.save(account);

        // update additional attributes in store
        // TODO

        // build identity
        // detach account
        account = accountRepository.detach(account);
        // rewrite internal userId
        account.setUserId(exportInternalId(userId));
        // TODO write custom model
        DefaultIdentityImpl identity = new DefaultIdentityImpl(SystemKeys.AUTHORITY_OIDC, provider, realm);
        identity.setAccount(account);
        identity.setAttributes(Collections.emptyList());
        return identity;
    }

    @Override
    public UserIdentity getIdentity(String userId) throws NoSuchUserException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserIdentity getIdentity(String userId, boolean fetchAttributes) throws NoSuchUserException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<UserIdentity> listIdentities(String subject) {
        // TODO handle not persisted configuration
        List<UserIdentity> identities = new ArrayList<>();
        String provider = getProvider();
        String realm = getRealm();
        List<OIDCUserAccount> accounts = accountRepository.findBySubjectAndRealmAndProvider(subject, realm,
                provider);

        for (OIDCUserAccount account : accounts) {
            // detach account
            account = accountRepository.detach(account);

            // TODO write custom model
            DefaultIdentityImpl identity = new DefaultIdentityImpl(SystemKeys.AUTHORITY_OIDC, provider, realm);
            identity.setAccount(account);
            identity.setAttributes(Collections.emptyList());

            identities.add(identity);
        }

        return identities;

    }

}
