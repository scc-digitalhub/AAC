package it.smartcommunitylab.aac.webauthn.provider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.AccountAttributesSet;
import it.smartcommunitylab.aac.attributes.BasicAttributesSet;
import it.smartcommunitylab.aac.attributes.EmailAttributesSet;
import it.smartcommunitylab.aac.attributes.OpenIdAttributesSet;
import it.smartcommunitylab.aac.core.base.AbstractIdentityAttributeProvider;
import it.smartcommunitylab.aac.core.base.DefaultUserAttributesImpl;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccount;

public class WebAuthnAttributeProvider
        extends AbstractIdentityAttributeProvider<WebAuthnUserAuthenticatedPrincipal, WebAuthnUserAccount> {

    public WebAuthnAttributeProvider(
            String providerId,
            WebAuthnIdentityProviderConfig providerConfig,
            String realm) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, providerId, realm);
        Assert.notNull(providerConfig, "provider config is mandatory");

        // disable attribute store
        this.attributeStore = null;
    }

    @Override
    protected List<UserAttributes> extractUserAttributes(WebAuthnUserAccount account,
            Map<String, Serializable> principalAttributes) {
        List<UserAttributes> attributes = new ArrayList<>();
//        String userId = exportInternalId(account.getUserId());
        String userId = account.getUserId();

        // build base
        BasicAttributesSet basicset = new BasicAttributesSet();
        basicset.setName(account.getName());
        basicset.setSurname(account.getSurname());
        basicset.setEmail(account.getEmailAddress());
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
        emailset.setEmail(account.getEmailAddress());
        emailset.setEmailVerified(account.isConfirmed());
        attributes.add(new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), userId,
                emailset));

        // openid fixed
        OpenIdAttributesSet openidset = new OpenIdAttributesSet();
        openidset.setPreferredUsername(account.getUsername());
        openidset.setName(account.getName());
        openidset.setGivenName(account.getName());
        openidset.setFamilyName(account.getSurname());
        openidset.setEmail(account.getEmailAddress());
        openidset.setEmailVerified(account.isConfirmed());
        attributes.add(new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(), userId,
                openidset));

        return attributes;
    }

}
