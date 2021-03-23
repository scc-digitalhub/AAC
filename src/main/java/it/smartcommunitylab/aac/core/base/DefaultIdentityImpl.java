package it.smartcommunitylab.aac.core.base;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.security.core.CredentialsContainer;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.profiles.model.BasicProfile;
import it.smartcommunitylab.aac.profiles.model.OpenIdProfile;

/*
 * Default Identity is an instantiable bean which contains account and identity
 */

public class DefaultIdentityImpl extends BaseIdentity implements CredentialsContainer {

    protected UserAccount account;
    private Collection<UserAttributes> attributes;

    public DefaultIdentityImpl(String authority, String provider, String realm) {
        super(authority, provider, realm);
        attributes = Collections.emptyList();
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
    public BasicProfile toBasicProfile() {
        BasicProfile profile = new BasicProfile();
        profile.setUsername(getAccount().getUsername());

        // lookup attributes with default names (oidc)
        profile.setName(getAttribute("given_name", "profile"));
        profile.setSurname(getAttribute("family_name", "profile"));
        profile.setEmail(getAttribute("email", "email", "profile"));

        return profile;
    }

    @Override
    public OpenIdProfile toOpenIdProfile() {
        OpenIdProfile profile = new OpenIdProfile();
        profile.setUsername(getAccount().getUsername());

        // lookup attributes with default names (oidc)
        profile.setGivenName(getAttribute("given_name", "profile"));
        profile.setFamilyName(getAttribute("family_name", "profile"));
        profile.setEmail(getAttribute("email", "email", "profile"));
        profile.setMiddleName(getAttribute("middle_name", "profile"));
        profile.setNickName(getAttribute("nickname", "profile"));
        profile.setPhone(getAttribute("phone_number", "phone", "profile"));
        profile.setProfileUrl(getAttribute("profile", "profile"));
        profile.setPictureUrl(getAttribute("picture", "profile"));
        profile.setWebsiteUrl(getAttribute("website", "profile"));
        profile.setGender(getAttribute("gender", "profile"));

        String emailVerifiedAttr = getAttribute("email_verified", "email", "profile");
        boolean emailVerified = emailVerifiedAttr != null ? Boolean.parseBoolean(emailVerifiedAttr) : false;
        profile.setEmailVerified(emailVerified);

        String phoneVerifiedAttr = getAttribute("phone_number_verified", "phone", "profile");
        boolean phoneVerified = phoneVerifiedAttr != null ? Boolean.parseBoolean(phoneVerifiedAttr) : false;
        profile.setPhoneVerified(phoneVerified);

        try {
            String birthdateAttr = getAttribute("birthdate", "profile");
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            Date birthdate = birthdateAttr != null ? formatter.parse(birthdateAttr) : null;
            profile.setBirthdate(birthdate);
        } catch (ParseException e) {
            profile.setBirthdate(null);
        }
        
        return profile;
    }

    @Override
    public String getUserId() {
        return account.getUserId();
    }

    // lookup an attribute in multiple sets, return first match
    private String getAttribute(String key, String... identifier) {
        Optional<String> attr = attributes.stream()
                .filter(a -> ArrayUtils.contains(identifier, a.getIdentifier()))
                .filter(a -> a.getAttributes().containsKey(key))
                .findFirst().map(a -> a.getAttributes().get(key));
        if (attr.isPresent()) {
            return attr.get();
        }

        return null;

    }

    public static final String ATTRIBUTES_EMAIL = "email";
    public static final String ATTRIBUTES_PROFILE = "profile";

    @Override
    public String toString() {
        return "DefaultIdentityImpl [account=" + account + ", attributes=" + attributes + "]";
    }

}
