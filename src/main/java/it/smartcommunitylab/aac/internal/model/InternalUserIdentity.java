package it.smartcommunitylab.aac.internal.model;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.model.StringAttribute;
import it.smartcommunitylab.aac.core.base.BaseIdentity;
import it.smartcommunitylab.aac.core.base.DefaultUserAttributesImpl;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.persistence.AttributeEntity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.profiles.model.BasicProfile;
import it.smartcommunitylab.aac.profiles.model.OpenIdProfile;

public class InternalUserIdentity extends BaseIdentity implements CredentialsContainer {

    // TODO use a global version as serial uid
    private static final long serialVersionUID = -2050916229494701678L;

    private InternalUserAccount account;

    private String userId;
    // we override providerid to be able to update it
    private String provider;

    // we keep attributes in flat map
    // TODO evaluate drop, we shouldn't have extra attributes
    private Map<String, String> attributes;

    public InternalUserIdentity(String authority, String provider, String realm, InternalUserAccount account) {
        super(authority, provider, realm);
        this.account = account;
        this.provider = provider;
    }

    @Override
    public UserAccount getAccount() {
        return account;
    }

    @Override
    public String getAuthority() {
        return SystemKeys.AUTHORITY_INTERNAL;
    }

    @Override
    public Collection<UserAttributes> getAttributes() {
        // manually map attributes into sets
        List<UserAttributes> attributes = new ArrayList<>();
        // TODO rework and move to attributeProvider, here we should simly store them
        String internalUserId = this.parseResourceId(userId);
        DefaultUserAttributesImpl profile = new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(),
                "profile");
        profile.setInternalUserId(internalUserId);
        profile.addAttribute(new StringAttribute("name", account.getName()));
        profile.addAttribute(new StringAttribute("surname", account.getSurname()));
        profile.addAttribute(new StringAttribute("email", account.getEmail()));
        attributes.add(profile);

        DefaultUserAttributesImpl email = new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(),
                "email");
        email.setInternalUserId(internalUserId);
        email.addAttribute(new StringAttribute("email", account.getEmail()));
        attributes.add(email);

        DefaultUserAttributesImpl username = new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(),
                "username");
        username.setInternalUserId(internalUserId);
        username.addAttribute(new StringAttribute("username", account.getUsername()));
        attributes.add(username);

        return attributes;
    }

    /*
     * we want this to be immutable
     */

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return account.getUsername();
    }

    public String getEmailAddress() {
        return account.getEmail();
    }

    /*
     * userid can be resetted to properly map identity
     */

    public void setUserId(String userId) {
        this.userId = userId;
    }

    // provider id can be resetted to properly map identity
    public void setProvider(String providerId) {
        this.provider = providerId;
    }

    public String getProvider() {
        return this.provider;
    }

    /*
     * Builder
     */
    public static InternalUserIdentity from(String provider, InternalUserAccount user, String realm) {
        InternalUserIdentity i = new InternalUserIdentity(SystemKeys.AUTHORITY_INTERNAL, provider, realm, user);
        i.userId = user.getUserId();

        // we do not copy password by default
        // let service handle the retrieval

        return i;

    }

    /*
     * Helpers
     */
    private static AbstractMap.SimpleEntry<String, String> createAttribute(String key, String value) {
        String k = buildAttributeKey(key);
        return new AbstractMap.SimpleEntry<>(k, value);

    }

    private static String buildAttributeKey(String key) {
        String k = key;
        if (StringUtils.hasText(k)) {
            if (!k.startsWith(ATTRIBUTE_PREFIX)) {
                k = ATTRIBUTE_PREFIX + key;
            }
        }

        return k;
    }

    private static final String ATTRIBUTE_PREFIX = SystemKeys.AUTHORITY_INTERNAL + ".";

    @Override
    public BasicProfile toBasicProfile() {
        BasicProfile profile = new BasicProfile();
        profile.setUsername(account.getUsername());
        profile.setName(account.getName());
        profile.setSurname(account.getSurname());
        profile.setEmail(account.getEmail());

        return profile;
    }

    @Override
    public OpenIdProfile toOpenIdProfile() {
        OpenIdProfile profile = new OpenIdProfile();
        profile.setUsername(account.getUsername());
        profile.setName(account.getName());
        profile.setGivenName(account.getName());
        profile.setFamilyName(account.getSurname());
        profile.setEmail(account.getEmail());

        return profile;
    }

    @Override
    public void eraseCredentials() {
        this.account.setPassword(null);
    }

    public Object getCredentials() {
        return this.account.getPassword();
    }

    @Override
    public String toString() {
        return "InternalUserIdentity [account=" + account + ", userId=" + userId + ", provider=" + provider + "]";
    }

}
