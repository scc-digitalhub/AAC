package it.smartcommunitylab.aac.openid;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.Constants;
import it.smartcommunitylab.aac.core.base.BaseIdentity;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.persistence.AttributeEntity;
import it.smartcommunitylab.aac.profiles.model.BasicProfile;

public class OIDCUserIdentity extends BaseIdentity implements Serializable {

    private static final long serialVersionUID = 2760694301395978161L;

    private String realm;
    private String userId;
    private String provider;
    private String username;
    private Set<AbstractMap.SimpleEntry<String, String>> attributes;

    protected OIDCUserIdentity() {
        attributes = Collections.emptySet();
    }

    @Override
    public String getAuthority() {
        return Constants.AUTHORITY_OIDC;
    }

    @Override
    public void eraseCredentials() {
        // we do not have credentials or sensible data

    }

    @Override
    public Object getCredentials() {
        // we do not have credentials or sensible data
        return null;
    }

    /*
     * props: only getters we want this to be immutable
     */
    public String getRealm() {
        return realm;
    }

    public String getUserId() {
        return userId;
    }

    public String getProvider() {
        return provider;
    }

    public String getUsername() {
        return username;
    }

    /*
     * Builder
     */
    public static OIDCUserIdentity from(OIDCUserAccount user, Collection<AttributeEntity> attributes) {
        OIDCUserIdentity i = new OIDCUserIdentity();
        i.realm = user.getRealm();
        i.provider = user.getProvider();
        i.userId = user.getUserId();
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

    private static final String ATTRIBUTE_PREFIX = Constants.AUTHORITY_OIDC + ".";

    @Override
    public String getEmailAddress() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getFirstName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getLastName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getFullName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BasicProfile toProfile() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserAttributes getAttributes() {
        // TODO Auto-generated method stub
        return null;
    }

}