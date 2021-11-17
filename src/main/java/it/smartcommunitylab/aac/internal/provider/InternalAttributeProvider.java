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
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.base.ConfigurableProperties;
import it.smartcommunitylab.aac.core.base.DefaultUserAttributesImpl;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.provider.AttributeProvider;
import it.smartcommunitylab.aac.internal.model.InternalUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.service.InternalUserAccountService;

public class InternalAttributeProvider extends AbstractProvider implements AttributeProvider {

    // services
    private final InternalUserAccountService userAccountService;
    private final InternalIdentityProviderConfig providerConfig;

    public InternalAttributeProvider(
            String providerId,
            InternalUserAccountService userAccountService,
            InternalIdentityProviderConfig providerConfig,
            String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId, realm);
        Assert.notNull(userAccountService, "account service is mandatory");
        Assert.notNull(providerConfig, "provider config is mandatory");

        this.userAccountService = userAccountService;
        this.providerConfig = providerConfig;

    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ATTRIBUTES;
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

    @Override
    public Collection<UserAttributes> convertAttributes(UserAuthenticatedPrincipal principal, String subjectId) {
        // we expect an instance of our model
        InternalUserAuthenticatedPrincipal user = (InternalUserAuthenticatedPrincipal) principal;
        String userId = user.getUserId();
        String username = parseResourceId(userId);
        String realm = getRealm();

        InternalUserAccount account = userAccountService.findAccountByUsername(realm, username);
        if (account == null) {
            return null;
        }

        return extractUserAttributes(account);

    }

    @Override
    public Collection<UserAttributes> getAttributes(String subjectId) {
        // we expect subjectId to be == userId
        String userId = subjectId;
        String username = parseResourceId(userId);
        String realm = getRealm();

        InternalUserAccount account = userAccountService.findAccountByUsername(realm, username);
        if (account == null) {
            return null;
        }

        return extractUserAttributes(account);
    }

    // TODO move to (idp) attributeProvider
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
    public void deleteAttributes(String subjectId) {
        // nothing to do
    }

}