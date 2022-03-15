package it.smartcommunitylab.aac.internal.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.AccountAttributesSet;
import it.smartcommunitylab.aac.attributes.BasicAttributesSet;
import it.smartcommunitylab.aac.attributes.EmailAttributesSet;
import it.smartcommunitylab.aac.attributes.OpenIdAttributesSet;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.base.DefaultUserAttributesImpl;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.provider.AttributeProvider;
import it.smartcommunitylab.aac.internal.model.InternalUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.service.InternalUserAccountService;

public class InternalAttributeProvider extends AbstractProvider implements AttributeProvider {

    // services
    private final InternalUserAccountService accountService;
    private final InternalIdentityProviderConfig config;

    public InternalAttributeProvider(
            String providerId,
            InternalUserAccountService userAccountService,
            InternalIdentityProviderConfig providerConfig,
            String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId, realm);
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(providerConfig, "provider config is mandatory");

        this.accountService = userAccountService;
        this.config = providerConfig;

    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ATTRIBUTES;
    }

    @Override
    public String getName() {
        return config.getName();
    }

    @Override
    public String getDescription() {
        return config.getDescription();
    }

    @Override
    public Collection<UserAttributes> convertAttributes(UserAuthenticatedPrincipal principal, String userId) {
        // we expect an instance of our model
        InternalUserAuthenticatedPrincipal user = (InternalUserAuthenticatedPrincipal) principal;
        String username = user.getUsername();

        InternalUserAccount account = accountService.findAccountByUsername(getProvider(), username);
        if (account == null) {
            return null;
        }

        return extractUserAttributes(account);
    }

    @Override
    public Collection<UserAttributes> getAttributes(String userId) {
        // nothing is accessible here by user, only by account
        return null;
    }

    public Collection<UserAttributes> getAccountAttributes(String username) {
        InternalUserAccount account = accountService.findAccountByUsername(getProvider(), username);
        if (account == null) {
            return null;
        }

        return extractUserAttributes(account);
    }

    private Collection<UserAttributes> extractUserAttributes(InternalUserAccount account) {
        List<UserAttributes> attributes = new ArrayList<>();
//        String userId = exportInternalId(account.getUserId());
        String userId = account.getUserId();

        // build base
        BasicAttributesSet basicset = new BasicAttributesSet();
        basicset.setName(account.getName());
        basicset.setSurname(account.getSurname());
        basicset.setEmail(account.getEmail());
        basicset.setUsername(account.getUsername());
        attributes.add(new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), userId,
                basicset));

        // account
        AccountAttributesSet accountset = new AccountAttributesSet();
        accountset.setUsername(account.getUsername());
        accountset.setUserId(account.getUserId());
        accountset.setId(account.getUsername());
        attributes.add(new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), userId,
                accountset));
        // email
        EmailAttributesSet emailset = new EmailAttributesSet();
        emailset.setEmail(account.getEmail());
        emailset.setEmailVerified(account.isConfirmed());
        attributes.add(new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), userId,
                emailset));

        // openid fixed
        OpenIdAttributesSet openidset = new OpenIdAttributesSet();
        openidset.setPreferredUsername(account.getUsername());
        openidset.setName(account.getName());
        openidset.setGivenName(account.getName());
        openidset.setFamilyName(account.getSurname());
        openidset.setEmail(account.getEmail());
        openidset.setEmailVerified(account.isConfirmed());
        attributes.add(new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), userId,
                openidset));

        return attributes;
    }

    @Override
    public void deleteAttributes(String userId) {
        // nothing to do
    }

}