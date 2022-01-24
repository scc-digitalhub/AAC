package it.smartcommunitylab.aac.webauthn.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.BasicAttributesSet;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.base.ConfigurableProperties;
import it.smartcommunitylab.aac.core.base.DefaultUserAttributesImpl;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.provider.AttributeProvider;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccount;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnUserAccountService;

public class WebAuthnAttributeProvider extends AbstractProvider implements AttributeProvider {
    private final WebAuthnUserAccountService userAccountService;
    private final WebAuthnIdentityProviderConfig providerConfig;

    public WebAuthnAttributeProvider(
            String providerId,
            WebAuthnUserAccountService userAccountService,
            WebAuthnIdentityProviderConfig providerConfig,
            String realm) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, providerId, realm);
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
    public Collection<UserAttributes> getAttributes(String subjectId) {
        // we expect subjectId to be == userId
        String userId = subjectId;
        String username = parseResourceId(userId);
        String provider = getProvider();

        WebAuthnUserAccount account;
        try {
            account = userAccountService.findByProviderAndUsername(provider, username);
        } catch (NoSuchUserException _e) {
            account = null;
        }
        if (account == null) {
            return null;
        }

        return extractUserAttributes(account);
    }

    private Collection<UserAttributes> extractUserAttributes(WebAuthnUserAccount account) {
        List<UserAttributes> attributes = new ArrayList<>();
        String userId = account.getUserId();

        // build base
        BasicAttributesSet basicset = new BasicAttributesSet();
        basicset.setEmail(account.getEmailAddress());
        basicset.setUsername(account.getUsername());
        attributes.add(new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), userId,
                basicset));
        return attributes;
    }

    @Override
    public Collection<UserAttributes> convertAttributes(UserAuthenticatedPrincipal principal, String subjectId) {
        // we expect an instance of our model
        WebAuthnUserAuthenticatedPrincipal user = (WebAuthnUserAuthenticatedPrincipal) principal;
        String userId = user.getUserId();
        String username = parseResourceId(userId);
        String provider = getProvider();

        WebAuthnUserAccount account;
        try {
            account = userAccountService.findByProviderAndUsername(provider, username);
        } catch (NoSuchUserException _e) {
            account = null;
        }
        if (account == null) {
            return null;
        }

        return extractUserAttributes(account);
    }

    @Override
    public void deleteAttributes(String subjectId) {
        // nothing to do
    }
}
