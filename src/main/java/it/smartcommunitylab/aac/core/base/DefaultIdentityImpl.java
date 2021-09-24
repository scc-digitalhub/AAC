package it.smartcommunitylab.aac.core.base;

import java.util.Collection;
import java.util.Collections;
import org.springframework.security.core.CredentialsContainer;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAttributes;

/*
 * Default Identity is an instantiable bean which contains account and identity
 */

public class DefaultIdentityImpl extends BaseIdentity implements CredentialsContainer {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected final UserAuthenticatedPrincipal principal;
    protected UserAccount account;
    protected Collection<UserAttributes> attributes;
    protected String username;

    public DefaultIdentityImpl(String authority, String provider, String realm) {
        super(authority, provider, realm);
        principal = null;
        attributes = Collections.emptyList();
    }

    public DefaultIdentityImpl(String authority, String provider, String realm, UserAuthenticatedPrincipal principal) {
        super(authority, provider, realm);
        attributes = Collections.emptyList();
        this.principal = principal;

    }

    @Override
    public UserAuthenticatedPrincipal getPrincipal() {
        return principal;
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
        if (attributes != null) {
            this.attributes = Collections.unmodifiableCollection(attributes);
        }
    }

    public String getUsername() {
        if (this.username != null) {
            return this.username;
        }

        return account.getUsername();
    }

    public void setUsername(String username) {
        this.username = username;
    }

    /*
     * TODO remove toProfile* from here and let profileExtractors handle from
     * attributeSets
     */

//    @Override
//    public BasicProfile toBasicProfile() {
//        BasicProfile profile = new BasicProfile();
//
//        // username is not modifiable via attributes
//        profile.setUsername(getAccount().getUsername());
//
//        // lookup attributes with default names in basic profile
//        String name = getAttribute(BasicProfileAttributesSet.NAME, BasicProfileAttributesSet.IDENTIFIER, "profile");
//        if (!StringUtils.hasText(name)) {
//            // fall back to openid profile
//            name = getAttribute(OpenIdProfileAttributesSet.GIVEN_NAME, OpenIdProfileAttributesSet.IDENTIFIER,
//                    "profile");
//        }
//        String surname = getAttribute(BasicProfileAttributesSet.SURNAME, BasicProfileAttributesSet.IDENTIFIER,
//                "profile");
//        if (!StringUtils.hasText(surname)) {
//            // fall back to openid profile
//            surname = getAttribute(OpenIdProfileAttributesSet.FAMILY_NAME, OpenIdProfileAttributesSet.IDENTIFIER,
//                    "profile");
//        }
//        String email = getAttribute(BasicProfileAttributesSet.EMAIL, BasicProfileAttributesSet.IDENTIFIER, "profile");
//        if (!StringUtils.hasText(email)) {
//            // fall back to openid profile
//            email = getAttribute(OpenIdProfileAttributesSet.EMAIL, OpenIdProfileAttributesSet.IDENTIFIER, "profile");
//        }
//
//        profile.setName(name);
//        profile.setSurname(surname);
//        profile.setEmail(email);
//
//        return profile;
//    }
//
//    @Override
//    public OpenIdProfile toOpenIdProfile() {
//        OpenIdProfile profile = new OpenIdProfile();
//
//        // username is not modifiable via attributes
//        profile.setUsername(getAccount().getUsername());
//
//        // lookup attributes with default names in openid profile
//        String givenName = getAttribute(OpenIdProfileAttributesSet.GIVEN_NAME, OpenIdProfileAttributesSet.IDENTIFIER,
//                "profile");
//        if (!StringUtils.hasText(givenName)) {
//            // fall back to basic profile
//            givenName = getAttribute(BasicProfileAttributesSet.NAME, BasicProfileAttributesSet.IDENTIFIER, "profile");
//        }
//        String familyName = getAttribute(OpenIdProfileAttributesSet.FAMILY_NAME, OpenIdProfileAttributesSet.IDENTIFIER,
//                "profile");
//        if (!StringUtils.hasText(familyName)) {
//            // fall back to basic profile
//            familyName = getAttribute(BasicProfileAttributesSet.SURNAME, BasicProfileAttributesSet.IDENTIFIER,
//                    "profile");
//        }
//        String email = getAttribute(OpenIdProfileAttributesSet.EMAIL, OpenIdProfileAttributesSet.IDENTIFIER, "email",
//                "profile");
//        if (!StringUtils.hasText(email)) {
//            // fall back to basic profile
//            email = getAttribute(BasicProfileAttributesSet.EMAIL, BasicProfileAttributesSet.IDENTIFIER, "profile");
//        }
//
//        profile.setGivenName(givenName);
//        profile.setFamilyName(familyName);
//        profile.setEmail(email);
//
//        // lookup attributes with default names (oidc)
//
//        profile.setMiddleName(
//                getAttribute(OpenIdProfileAttributesSet.MIDDLE_NAME, OpenIdProfileAttributesSet.IDENTIFIER, "profile"));
//        profile.setNickName(
//                getAttribute(OpenIdProfileAttributesSet.NICKNAME, OpenIdProfileAttributesSet.IDENTIFIER, "profile"));
//        profile.setPhone(getAttribute(OpenIdProfileAttributesSet.PHONE_NUMBER, OpenIdProfileAttributesSet.IDENTIFIER,
//                "phone", "profile"));
//        profile.setProfileUrl(
//                getAttribute(OpenIdProfileAttributesSet.PROFILE, OpenIdProfileAttributesSet.IDENTIFIER, "profile"));
//        profile.setPictureUrl(
//                getAttribute(OpenIdProfileAttributesSet.PICTURE, OpenIdProfileAttributesSet.IDENTIFIER, "profile"));
//        profile.setWebsiteUrl(
//                getAttribute(OpenIdProfileAttributesSet.WEBSITE, OpenIdProfileAttributesSet.IDENTIFIER, "profile"));
//        profile.setGender(
//                getAttribute(OpenIdProfileAttributesSet.GENDER, OpenIdProfileAttributesSet.IDENTIFIER, "profile"));
//
//        String emailVerifiedAttr = getAttribute(OpenIdProfileAttributesSet.EMAIL_VERIFIED,
//                OpenIdProfileAttributesSet.IDENTIFIER, "email",
//                "profile");
//        boolean emailVerified = emailVerifiedAttr != null ? Boolean.parseBoolean(emailVerifiedAttr) : false;
//        profile.setEmailVerified(emailVerified);
//
//        String phoneVerifiedAttr = getAttribute(OpenIdProfileAttributesSet.PHONE_NUMBER_VERIFIED,
//                OpenIdProfileAttributesSet.IDENTIFIER, "phone",
//                "profile");
//        boolean phoneVerified = phoneVerifiedAttr != null ? Boolean.parseBoolean(phoneVerifiedAttr) : false;
//        profile.setPhoneVerified(phoneVerified);
//
//        try {
//            String birthdateAttr = getAttribute(OpenIdProfileAttributesSet.BIRTHDATE,
//                    OpenIdProfileAttributesSet.IDENTIFIER, "profile");
//            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
//            Date birthdate = birthdateAttr != null ? formatter.parse(birthdateAttr) : null;
//            profile.setBirthdate(birthdate);
//        } catch (ParseException e) {
//            profile.setBirthdate(null);
//        }
//
//        String zoneInfo = getAttribute(OpenIdProfileAttributesSet.ZONEINFO, OpenIdProfileAttributesSet.IDENTIFIER,
//                "profile");
//        if (!StringUtils.hasText(zoneInfo)) {
//            zoneInfo = TimeZone.getDefault().getDisplayName();
//        }
//        profile.setZoneinfo(zoneInfo);
//
//        String locale = getAttribute(OpenIdProfileAttributesSet.LOCALE, OpenIdProfileAttributesSet.IDENTIFIER,
//                "profile");
//        if (!StringUtils.hasText(locale)) {
//            locale = Locale.getDefault().getDisplayName();
//        }
//        profile.setLocale(locale);
//
//        return profile;
//    }

    @Override
    public String getUserId() {
        return account.getUserId();
    }

//    // lookup an attribute in multiple sets, return first match
//    private String getAttribute(String key, String... identifier) {
//        Set<UserAttributes> sets = attributes.stream()
//                .filter(a -> ArrayUtils.contains(identifier, a.getIdentifier()))
//                .collect(Collectors.toSet());
//
//        for (UserAttributes uattr : sets) {
//            Optional<Attribute> attr = uattr.getAttributes().stream().filter(a -> a.getKey().equals(key)).findFirst();
//            if (attr.isPresent()) {
//                return attr.get().getValue().toString();
//            }
//        }
//
//        return null;
//
//    }

//    public static final String ATTRIBUTES_EMAIL = "email";
//    public static final String ATTRIBUTES_PROFILE = "profile";

    @Override
    public String toString() {
        return "DefaultIdentityImpl [account=" + account + ", attributes=" + attributes + "]";
    }

}
