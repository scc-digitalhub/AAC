package it.smartcommunitylab.aac.internal.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractAuthenticatedPrincipal;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalUserAuthenticatedPrincipal extends AbstractAuthenticatedPrincipal {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private String name;
    private Boolean confirmed;

    // internal attributes from account
    private Map<String, String> attributes;

    public InternalUserAuthenticatedPrincipal(String authority, String provider, String realm, String userId,
            String username) {
        super(authority, provider, realm, userId);
        Assert.hasText(username, "username can not be null or empty");
        this.username = username;

    }

    public InternalUserAuthenticatedPrincipal(String provider, String realm, String userId, String username) {
        this(SystemKeys.AUTHORITY_INTERNAL, provider, realm, userId, username);
    }

    @Override
    public String getId() {
        return username;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isEmailVerified() {
        return isEmailConfirmed();
    }

    @Override
    public Map<String, Serializable> getAttributes() {
        Map<String, Serializable> map = new HashMap<>();
        map.put("authority", getAuthority());
        map.put("provider", getProvider());
        map.put("username", username);
        map.put("id", username);

        if (StringUtils.hasText(name)) {
            map.put("name", name);
        }

        String userId = getUserId();
        if (StringUtils.hasText(userId)) {
            map.put("userId", userId);
        }

        // add all account attributes if set
        if (attributes != null) {
            map.putAll(attributes);
        }

        // override if set
        if (StringUtils.hasText(emailAddress)) {
            map.put("email", emailAddress);
        }
        if (confirmed != null) {
            map.put("confirmed", Boolean.toString(confirmed.booleanValue()));
        }

        return map;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getEmail() {
        return emailAddress;
    }

    public void setEmail(String email) {
        this.emailAddress = email;
    }

    public boolean isEmailConfirmed() {
        boolean verified = confirmed != null ? confirmed.booleanValue() : false;
        return StringUtils.hasText(emailAddress) && verified;
    }

    public Boolean getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(Boolean confirmed) {
        this.confirmed = confirmed;
    }

    public void setAccountAttributes(InternalUserAccount account) {
        if (account != null) {
            this.emailAddress = account.getEmail();
            this.confirmed = account.isConfirmed();

            // map base attributes, these will be available for custom mapping
            attributes = new HashMap<>();

            String pname = account.getName();
            if (StringUtils.hasText(pname)) {
                attributes.put("name", pname);
            }

            String surname = account.getSurname();
            if (StringUtils.hasText(surname)) {
                attributes.put("surname", surname);
            }

            String lang = account.getLang();
            if (StringUtils.hasText(lang)) {
                attributes.put("lang", lang);
            }
        }
    }

    public void setName(String name) {
        this.name = name;
    }

}
