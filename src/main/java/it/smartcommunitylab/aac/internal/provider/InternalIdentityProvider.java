package it.smartcommunitylab.aac.internal.provider;

import java.util.Collection;

import org.springframework.util.Assert;
import org.springframework.util.SerializationUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.persistence.RoleEntityRepository;
import it.smartcommunitylab.aac.core.persistence.UserEntity;
import it.smartcommunitylab.aac.core.provider.AccountProvider;
import it.smartcommunitylab.aac.core.provider.AttributeProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccountRepository;
import it.smartcommunitylab.aac.model.Subject;

public class InternalIdentityProvider extends AbstractProvider implements IdentityProvider {

    private final UserEntityService userService;

    private final InternalUserAccountRepository userRepository;
    private final RoleEntityRepository roleRepository;

    private final InternalAccountProvider accountProvider;
    private final InternalAttributeProvider attributeProvider;
    private final InternalAuthenticationProvider authenticationProvider;
    private final InternalSubjectResolver subjectResolver;

    public InternalIdentityProvider(
            String providerId,
            UserEntityService userService,
            InternalUserAccountRepository userRepository,
            RoleEntityRepository roleRepository, String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId, realm);
        Assert.notNull(userService, "user service is mandatory");
        Assert.notNull(userRepository, "account repository is mandatory");
        Assert.notNull(roleRepository, "role repository is mandatory");

        // we need user service to manage subjects
        this.userService = userService;

        // internal data repositories
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;

        // build resource providers, we use our providerId to ensure consistency
        this.accountProvider = new InternalAccountProvider(providerId, userRepository, realm);
        // TODO attributeService to feed attribute provider
        this.attributeProvider = new InternalAttributeProvider(providerId, userRepository, null, realm);
        this.authenticationProvider = new InternalAuthenticationProvider(providerId, userRepository, roleRepository,
                realm);
        this.subjectResolver = new InternalSubjectResolver(providerId, userRepository, realm);

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
    public UserIdentity convertIdentity(UserAuthenticatedPrincipal principal) throws NoSuchUserException {
        // extract account and attributes in raw format from authenticated principal
        String userId = principal.getUserId();

        // get a detached internal account entity
        InternalUserAccount account = accountProvider.getInternalAccount(userId);
        //re-set providerId since all internal accounts have the same
        account.setProvider(getProvider());

        String username = account.getUsername();
        // subjectId is always present, is derived from the same account table
        Subject subject = subjectResolver.resolveByUserId(userId);
        String subjectId = subject.getSubjectId();
        // make sure it is registered, and fetch the name
        UserEntity user = userService.getUser(subjectId);
        if (user == null) {
            // should not happen, but create as new
            user = userService.addUser(subjectId, username);
        }

        // store and update attributes
        // TODO

        // use builder to properly map attributes
        // TODO consolidate *all* attribute sets logic in attributeProvider
        InternalUserIdentity identity = InternalUserIdentity.from(getProvider(), account, getRealm());
        // we replace userId with the global one, forget the internal
        identity.setUserId(userId);
        

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

    /*
     * expose repositories
     */
    public InternalUserAccountRepository getUserRepository() {
        return userRepository;
    }

    public RoleEntityRepository getRoleRepository() {
        return roleRepository;
    }

}
