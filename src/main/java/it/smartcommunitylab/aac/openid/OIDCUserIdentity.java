package it.smartcommunitylab.aac.openid;

import java.io.Serializable;
import java.util.Collections;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.DefaultIdentityImpl;
import it.smartcommunitylab.aac.openid.auth.OIDCAuthenticatedPrincipal;

public class OIDCUserIdentity extends DefaultIdentityImpl implements Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_OIDC_SERIAL_VERSION;

    private String emailAddress;
    private OIDCAuthenticatedPrincipal principal;

    public OIDCUserIdentity(String provider, String realm) {
        super(SystemKeys.AUTHORITY_OIDC, provider, realm);
        attributes = Collections.emptySet();
    }

    public OIDCUserIdentity(String provider, String realm, OIDCAuthenticatedPrincipal principal) {
        super(SystemKeys.AUTHORITY_OIDC, provider, realm, principal);
        attributes = Collections.emptySet();
        this.principal = principal;
        this.username = principal.getName();
    }

    @Override
    public String getAuthority() {
        return SystemKeys.AUTHORITY_OIDC;
    }

    @Override
    public void eraseCredentials() {
        // we do not have credentials or sensible data

    }

    @Override
    public OIDCAuthenticatedPrincipal getPrincipal() {
        return this.principal;
    }

//    @Override
//    public Object getCredentials() {
//        // we do not have credentials or sensible data
//        return null;
//    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    /*
     * Builder
     */
//    public static OIDCUserIdentity from(OIDCUserAccount user, Collection<AttributeEntity> attributes) {
//        OIDCUserIdentity i = new OIDCUserIdentity(user.getProvider(), user.getRealm());
//        i.account = user;
//        i.username = user.getUsername();
//
//        // TODO build attributes
////        Set<AbstractMap.SimpleEntry<String, String>> attrs = new HashSet<>();
////        // static map base attrs
////        attrs.add(createAttribute("email", user.getEmail()));
////        attrs.add(createAttribute("email_verified", Boolean.toString(user.getEmailVerified())));
////        attrs.add(createAttribute("name", user.getName()));
////        attrs.add(createAttribute("given_name", user.getGivenName()));
////        attrs.add(createAttribute("family_name", user.getFamilyName()));
////        attrs.add(createAttribute("profile", user.getProfileUri()));
////        attrs.add(createAttribute("picture", user.getPictureUri()));
////
////        // also map additional attrs
////        attrs.addAll(
////                attributes.stream().map(a -> createAttribute(a.getKey(), a.getValue())).collect(Collectors.toSet()));
////
////        // filter empty or null attributes
////        i.attributes = attrs.stream().filter(a -> StringUtils.hasText(a.getValue())).collect(Collectors.toSet());
//
//        return i;
//
//    }

//    /*
//     * Helpers
//     */
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
//    private static final String ATTRIBUTE_PREFIX = SystemKeys.AUTHORITY_OIDC + ".";

}