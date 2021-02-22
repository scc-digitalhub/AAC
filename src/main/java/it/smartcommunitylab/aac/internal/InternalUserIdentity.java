package it.smartcommunitylab.aac.internal;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.Constants;
import it.smartcommunitylab.aac.core.base.BaseAttributes;
import it.smartcommunitylab.aac.core.base.BaseIdentity;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.persistence.AttributeEntity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.profiles.model.BasicProfile;

public class InternalUserIdentity extends BaseIdentity implements UserDetails, Serializable {

    // TODO use a global version as serial uid
    private static final long serialVersionUID = -2050916229494701678L;

    // TODO make fields final
    private String realm;
    private String userId;
    private String provider;
    private String username;
    private String email;
    // not immutable field
    private String password;

    // we keep attributes in base map impl
    private BaseAttributes attributes;

    protected InternalUserIdentity() {
        attributes = null;
    }

    @Override
    public String getAuthority() {
        return Constants.AUTHORITY_INTERNAL;
    }

    @Override
    public String getCredentials() {
        return password;
    }

    @Override
    public UserAttributes getAttributes() {
        return attributes;
    }

    @Override
    public void eraseCredentials() {
        // clear
        this.password = null;
    }

    /*
     * props: only getters, we want this to be immutable
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

    public String getEmailAddress() {
        return email;
    }

    /*
     * Builder
     */
    public static InternalUserIdentity from(InternalUserAccount user) {
        InternalUserIdentity i = new InternalUserIdentity();
        i.realm = user.getRealm();
        i.provider = user.getProvider();
        i.userId = user.getUserId();
        i.username = user.getUsername();
        i.email = user.getEmail();

        // we do not copy password by default
        // let service handle the retrieval

        Set<AbstractMap.SimpleEntry<String, String>> attrs = new HashSet<>();
        // static map base attrs
        attrs.add(createAttribute("username", user.getUsername()));
        attrs.add(createAttribute("email", user.getEmail()));
        attrs.add(createAttribute("name", user.getName()));
        attrs.add(createAttribute("surname", user.getSurname()));
        attrs.add(createAttribute("lang", user.getLang()));
        attrs.add(createAttribute("confirmed", Boolean.toString(user.isConfirmed())));

        // filter empty or null attributes and build bag
        i.attributes = new BaseAttributes(
                Constants.AUTHORITY_INTERNAL, i.provider,
                attrs.stream().filter(a -> StringUtils.hasText(a.getValue())).collect(Collectors.toSet()));

        return i;

    }

    public static InternalUserIdentity from(InternalUserAccount user, Collection<AttributeEntity> attributes) {
        InternalUserIdentity i = from(user);

        Set<Map.Entry<String, String>> attrs = new HashSet<>();
        // add base attributes
        attrs.addAll(i.getAttributes().getAttributes().entrySet());
        // also map additional attrs, we will reset provider
        attrs.addAll(
                attributes.stream().map(a -> createAttribute(a.getKey(), a.getValue())).collect(Collectors.toSet()));

        // filter empty or null attributes and build bag
        i.attributes = new BaseAttributes(
                Constants.AUTHORITY_INTERNAL, i.provider,
                attrs.stream().filter(a -> StringUtils.hasText(a.getValue())).collect(Collectors.toSet()));

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

    @Override
    public String getFirstName() {
        return attributes.getAttribute("name");
    }

    @Override
    public String getLastName() {
        return attributes.getAttribute("surname");
    }

    @Override
    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(attributes.getAttribute("name"))) {
            sb.append(attributes.getAttribute("name")).append(" ");
        }
        if (StringUtils.hasText(attributes.getAttribute("surname"))) {
            sb.append(attributes.getAttribute("surname"));
        }
        return sb.toString().trim();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // not provided here
        return null;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public BasicProfile toProfile() {
        BasicProfile profile = new BasicProfile();
        profile.setUsername(username);
        profile.setName(getFirstName());
        profile.setSurname(getLastName());
        profile.setEmail(email);

        return profile;
    }

}
