package it.smartcommunitylab.aac.internal;

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
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;

public class InternalUserIdentity extends BaseIdentity {

    private String realm;
    private String userId;
    private String provider;
    private String username;
    private String email;

    private Set<AbstractMap.SimpleEntry<String, String>> attributes;

    protected InternalUserIdentity() {
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<AbstractMap.SimpleEntry<String, String>> getAttributes() {
        return attributes;
    }

    public void setAttributes(Set<AbstractMap.SimpleEntry<String, String>> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getAuthority() {
        return Constants.AUTHORITY_INTERNAL;
    }

    /*
     * Builder
     */
    public static InternalUserIdentity from(InternalUserAccount user) {
        InternalUserIdentity i = new InternalUserIdentity();
        i.setRealm(user.getRealm());
        i.setProvider(user.getProvider());
        i.setUserId(user.getUserId());
        i.setUsername(user.getUsername());
        i.setEmail(user.getEmail());

        Set<AbstractMap.SimpleEntry<String, String>> attrs = new HashSet<>();
        // static map base attrs
        attrs.add(createAttribute("username", user.getUsername()));
        attrs.add(createAttribute("email", user.getEmail()));
        attrs.add(createAttribute("name", user.getName()));
        attrs.add(createAttribute("surname", user.getSurname()));
        attrs.add(createAttribute("lang", user.getLang()));
        attrs.add(createAttribute("confirmed", Boolean.toString(user.isConfirmed())));

        // filter empty or null attributes
        i.setAttributes(attrs.stream().filter(a -> StringUtils.hasText(a.getValue())).collect(Collectors.toSet()));

        return i;

    }

    public static InternalUserIdentity from(InternalUserAccount user, Collection<AttributeEntity> attributes) {
        InternalUserIdentity i = from(user);

        // also map additional attrs
        Set<AbstractMap.SimpleEntry<String, String>> attrs = i.getAttributes();
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

    private static final String ATTRIBUTE_PREFIX = Constants.AUTHORITY_INTERNAL + ".";
}
