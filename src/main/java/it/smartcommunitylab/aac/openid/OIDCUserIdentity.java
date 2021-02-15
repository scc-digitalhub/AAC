package it.smartcommunitylab.aac.openid;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.Constants;
import it.smartcommunitylab.aac.core.base.BaseIdentity;
import it.smartcommunitylab.aac.core.persistence.AttributeEntity;

public class OIDCUserIdentity extends BaseIdentity {

    private String realm;
    private String userId;
    private String provider;
    private String username;
    private Set<AbstractMap.SimpleEntry<String, String>> attributes;

    protected OIDCUserIdentity() {
        attributes = Collections.emptySet();
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Set<AbstractMap.SimpleEntry<String, String>> getAttributes() {
        return attributes;
    }

    public void setAttributes(Set<AbstractMap.SimpleEntry<String, String>> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getAuthority() {
        return Constants.AUTHORITY_OIDC;
    }

    /*
     * Builder
     */
    public static OIDCUserIdentity from(OIDCUserAccount user, Collection<AttributeEntity> attributes) {
        OIDCUserIdentity i = new OIDCUserIdentity();
        i.setRealm(user.getRealm());
        i.setProvider(user.getProvider());
        i.setUserId(user.getUserId());
        i.setUsername(user.getUsername());

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
        i.setAttributes(attrs.stream().filter(a -> StringUtils.hasText(a.getValue())).collect(Collectors.toSet()));

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
}