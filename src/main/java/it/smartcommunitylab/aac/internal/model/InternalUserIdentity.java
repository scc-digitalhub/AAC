package it.smartcommunitylab.aac.internal.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.core.CredentialsContainer;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.base.BaseIdentity;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.profiles.model.BasicProfile;

public class InternalUserIdentity extends BaseIdentity implements CredentialsContainer {

    // use a global version as serial uid
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final InternalUserAuthenticatedPrincipal principal;

    private InternalUserAccount account;

    private String userId;
//    // we override providerid to be able to update it
//    private String provider;

    // we keep attributes in flat map
    // TODO evaluate drop, we shouldn't have extra attributes
//    private Map<String, String> attributesMap;

    private Map<String, UserAttributes> attributes;

    public InternalUserIdentity(String provider, String realm, InternalUserAccount account) {
        super(SystemKeys.AUTHORITY_INTERNAL, provider, realm);
        this.account = account;
//        this.provider = provider;
        this.principal = null;
        this.userId = account.getUserId();
        this.attributes = new HashMap<>();
    }

    public InternalUserIdentity(String provider, String realm, InternalUserAccount account,
            InternalUserAuthenticatedPrincipal principal) {
        super(SystemKeys.AUTHORITY_INTERNAL, provider, realm);
        this.account = account;
//        this.provider = provider;
        this.principal = principal;
        this.userId = account.getUserId();
        this.attributes = new HashMap<>();

    }

    @Override
    public UserAuthenticatedPrincipal getPrincipal() {
        return principal;
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
//        // manually map attributes into sets
//        List<UserAttributes> attributes = new ArrayList<>();
//        // TODO rework and move to attributeProvider, here we should simly store them
//        DefaultUserAttributesImpl profile = new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(),
//                "profile");
//        profile.setUserId(userId);
//        profile.addAttribute(new StringAttribute("name", account.getName()));
//        profile.addAttribute(new StringAttribute("surname", account.getSurname()));
//        profile.addAttribute(new StringAttribute("email", account.getEmail()));
//        attributes.add(profile);
//
//        DefaultUserAttributesImpl email = new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(),
//                "email");
//        email.setUserId(userId);
//        email.addAttribute(new StringAttribute("email", account.getEmail()));
//        attributes.add(email);
//
//        DefaultUserAttributesImpl username = new DefaultUserAttributesImpl(getAuthority(), getProvider(), getRealm(),
//                "username");
//        username.setUserId(userId);
//        username.addAttribute(new StringAttribute("username", account.getUsername()));
//        attributes.add(username);

        return attributes.values();
    }

    /*
     * we want this to be immutable
     */

    public void setAttributes(Collection<UserAttributes> attributes) {
        this.attributes = new HashMap<>();
        if (attributes != null) {
            attributes.forEach(a -> this.attributes.put(a.getIdentifier(), a));
        }
    }

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

//    // provider id can be resetted to properly map identity
//    public void setProvider(String providerId) {
//        this.provider = providerId;
//    }
//
//    public String getProvider() {
//        return this.provider;
//    }
//
//    /*
//     * Builder
//     */
//    public static InternalUserIdentity from(String provider, InternalUserAccount user, String realm) {
//        InternalUserIdentity i = new InternalUserIdentity(SystemKeys.AUTHORITY_INTERNAL, provider, realm, user);
//        i.userId = user.getUserId();
//
//        // we do not copy password by default
//        // let service handle the retrieval
//
//        return i;
//
//    }

    /*
     * Helpers
     */
//    private static AbstractMap.SimpleEntry<String, String> createAttribute(String key, String value) {
//        String k = buildAttributeKey(key);
//        return new AbstractMap.SimpleEntry<>(k, value);
//
//    }
//
//    private static String buildAttributeKey(String key) {
//        String k = key;
//        if (StringUtils.hasText(k)) {
//            if (!k.startsWith(ATTRIBUTE_PREFIX)) {
//                k = ATTRIBUTE_PREFIX + key;
//            }
//        }
//
//        return k;
//    }
//
//    private static final String ATTRIBUTE_PREFIX = SystemKeys.AUTHORITY_INTERNAL + ".";

//    @Override
    public BasicProfile toBasicProfile() {
        BasicProfile profile = new BasicProfile();
        profile.setUsername(account.getUsername());
        profile.setName(account.getName());
        profile.setSurname(account.getSurname());
        profile.setEmail(account.getEmail());

        return profile;
    }

//    @Override
//    public OpenIdProfile toOpenIdProfile() {
//        OpenIdProfile profile = new OpenIdProfile();
//        profile.setUsername(account.getUsername());
//        profile.setName(account.getName());
//        profile.setGivenName(account.getName());
//        profile.setFamilyName(account.getSurname());
//        profile.setEmail(account.getEmail());
//
//        return profile;
//    }

    @Override
    public void eraseCredentials() {
        this.account.setPassword(null);
        if (this.principal != null) {
            this.principal.eraseCredentials();
        }
    }

    public Object getCredentials() {
        return this.account.getPassword();
    }

    @Override
    public String toString() {
        return "InternalUserIdentity [account=" + account + ", userId=" + userId + "]";
    }

}
