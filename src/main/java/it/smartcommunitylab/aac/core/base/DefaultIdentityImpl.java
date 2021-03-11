package it.smartcommunitylab.aac.core.base;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.security.core.CredentialsContainer;

import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.profiles.model.BasicProfile;

/*
 * Default Identity is an instantiable bean which contains account and identity
 */

public class DefaultIdentityImpl extends BaseIdentity implements CredentialsContainer {

    private UserAccount account;
    private Collection<UserAttributes> attributes;

    public DefaultIdentityImpl(String authority, String provider, String realm) {
        super(authority, provider, realm);
    }

    @Override
    public UserAccount getAccount() {
        return account;
    }

    public void setAccount(UserAccount account) {
        this.account = account;
    }

    public Collection<UserAttributes> getAttributes() {
        return attributes;
    }

    public void setAttributes(Collection<UserAttributes> attributes) {
        this.attributes = attributes;
    }

    @Override
    public BasicProfile toProfile() {
        BasicProfile profile = new BasicProfile();
        profile.setUsername(getAccount().getUsername());

        // lookup attributes with default names (oidc)
        Optional<String> name = getAttribute("given_name", "profile");
        Optional<String> surname = getAttribute("family_name", "profile");
        Optional<String> email = getAttribute("email", "email", "profile");

        if (name.isPresent()) {
            profile.setName(name.get());
        }
        if (surname.isPresent()) {
            profile.setSurname(surname.get());
        }
        if (email.isPresent()) {
            profile.setEmail(email.get());
        }

        return profile;
    }

    @Override
    public String getUserId() {
        return account.getUserId();
    }

    // lookup an attribute in multiple sets, return first match
    private Optional<String> getAttribute(String key, String... identifier) {
        return attributes.stream()
                .filter(a -> ArrayUtils.contains(identifier, a.getIdentifier()))
                .filter(a -> a.getAttributes().containsKey(key))
                .findFirst().map(a -> a.getAttributes().get(key));

    }

    public static final String ATTRIBUTES_EMAIL = "email";
    public static final String ATTRIBUTES_PROFILE = "profile";

    @Override
    public String toString() {
        return "DefaultIdentityImpl [account=" + account + ", attributes=" + attributes + "]";
    }

}
