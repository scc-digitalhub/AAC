package it.smartcommunitylab.aac.openid;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.BaseIdentity;
import it.smartcommunitylab.aac.core.base.DefaultIdentityImpl;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.persistence.AttributeEntity;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import it.smartcommunitylab.aac.profiles.model.BasicProfile;

public class OIDCUserIdentity extends DefaultIdentityImpl implements Serializable {

    private static final long serialVersionUID = 2760694301395978161L;

    private String username;
    private Set<AbstractMap.SimpleEntry<String, String>> attributes;

    protected OIDCUserIdentity(String provider, String realm) {
        super(SystemKeys.AUTHORITY_OIDC, provider, realm);
        attributes = Collections.emptySet();
    }

    @Override
    public String getAuthority() {
        return SystemKeys.AUTHORITY_OIDC;
    }

    @Override
    public void eraseCredentials() {
        // we do not have credentials or sensible data

    }

//    @Override
//    public Object getCredentials() {
//        // we do not have credentials or sensible data
//        return null;
//    }

    /*
     * props: only getters we want this to be immutable
     */
    public String getUsername() {
        return username;
    }



    /*
     * Builder
     */
    public static OIDCUserIdentity from(OIDCUserAccount user, Collection<AttributeEntity> attributes) {
        OIDCUserIdentity i = new OIDCUserIdentity(user.getProvider(), user.getRealm());
        i.account = user;
        i.username = user.getUsername();

        Set<AbstractMap.SimpleEntry<String, String>> attrs = new HashSet<>();
        // static map base attrs
        attrs.add(createAttribute("email", user.getEmail()));
        attrs.add(createAttribute("email_verified", Boolean.toString(user.getEmailVerified())));
        attrs.add(createAttribute("name", user.getName()));
        attrs.add(createAttribute("given_name", user.getGivenName()));
        attrs.add(createAttribute("family_name", user.getFamilyName()));
        attrs.add(createAttribute("profile", user.getProfileUri()));
        attrs.add(createAttribute("picture", user.getPictureUri()));

        // also map additional attrs
        attrs.addAll(
                attributes.stream().map(a -> createAttribute(a.getKey(), a.getValue())).collect(Collectors.toSet()));

        // filter empty or null attributes
        i.attributes = attrs.stream().filter(a -> StringUtils.hasText(a.getValue())).collect(Collectors.toSet());

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

    private static final String ATTRIBUTE_PREFIX = SystemKeys.AUTHORITY_OIDC + ".";

}