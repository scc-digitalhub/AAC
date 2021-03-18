package it.smartcommunitylab.aac.internal.provider;

import java.util.Collection;

import org.springframework.util.Assert;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.AccountProvider;
import it.smartcommunitylab.aac.core.provider.AttributeProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccountRepository;

public class InternalIdentityProvider extends AbstractProvider implements IdentityProvider {

    // services
    private final InternalUserAccountRepository accountRepository;

    // provider configuration
    private final InternalAccountProvider accountProvider;
    private final InternalAttributeProvider attributeProvider;
    private final InternalAuthenticationProvider authenticationProvider;
    private final InternalSubjectResolver subjectResolver;

    public InternalIdentityProvider(
            String providerId,
            InternalUserAccountRepository accountRepository,
            String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId, realm);
        Assert.notNull(accountRepository, "account repository is mandatory");

        // internal data repositories
        this.accountRepository = accountRepository;

        // build resource providers, we use our providerId to ensure consistency
        this.accountProvider = new InternalAccountProvider(providerId, accountRepository, realm);
        // TODO attributeService to feed attribute provider
        this.attributeProvider = new InternalAttributeProvider(providerId, accountRepository, null, realm);
        this.authenticationProvider = new InternalAuthenticationProvider(providerId, accountRepository, realm);
        this.subjectResolver = new InternalSubjectResolver(providerId, accountRepository, realm);

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

    @Override
    public UserIdentity convertIdentity(UserAuthenticatedPrincipal principal, String subjectId)
            throws NoSuchUserException {
        // extract account and attributes in raw format from authenticated principal
        String userId = principal.getUserId();
        String username = principal.getName();

        // userId should be username, check
        if (!parseResourceId(userId).equals(username)) {
            throw new NoSuchUserException();
        }

        if (subjectId == null) {
            // this better exists
            throw new NoSuchUserException();

        }

        // get the internal account entity
        InternalUserAccount account = accountRepository.findByRealmAndUsername(getRealm(), username);

        if (account == null) {
            // error, user should already exists for authentication
            throw new NoSuchUserException();
        }

        // subjectId is always present, is derived from the same account table
        String curSubjectId = account.getSubject();

        if (!curSubjectId.equals(subjectId)) {
            // force link
            // TODO re-evaluate
            account.setSubject(subjectId);
            account = accountRepository.save(account);
        }

        // detach account
        account = accountRepository.detach(account);

        // set providerId since all internal accounts have the same
        account.setProvider(getProvider());

        // rewrite internal userId
        account.setUserId(exportInternalId(username));

        // store and update attributes
        // TODO, we shouldn't have additional attributes for internal

        // use builder to properly map attributes
        // TODO consolidate *all* attribute sets logic in attributeProvider
        InternalUserIdentity identity = InternalUserIdentity.from(getProvider(), account, getRealm());

        // do note returned identity has credentials populated
        // consumers will need to eraseCredentials
        // TODO evaluate erase here
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
        // TODO Auto-generated method stub
        return null;
    }

    public void shutdown() {
        // cleanup ourselves
        // nothing to do
    }

}
