package it.smartcommunitylab.aac.webauthn.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractAuthenticatedPrincipal;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccount;

public class WebAuthnUserAuthenticatedPrincipal extends AbstractAuthenticatedPrincipal implements CredentialsContainer {

    private static final long serialVersionUID = SystemKeys.AAC_WEBAUTHN_SERIAL_VERSION;

    private final String username;

    private String uuid;

    private String name;
    private String emailAddress;
    private Boolean confirmed;

    // internal attributes from account
    private Map<String, String> attributes;

    public WebAuthnUserAuthenticatedPrincipal(String provider, String realm, String userId, String username) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, provider, realm, userId);
        Assert.hasText(username, "username can not be null or empty");
        this.username = username;

    }

    @Override
    public String getId() {
        return username;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getEmailAddress() {
        return emailAddress;
    }

    @Override
    public boolean isEmailVerified() {
        return isEmailConfirmed();
    }

    @Override
    public Map<String, Serializable> getAttributes() {
        Map<String, Serializable> map = new HashMap<>();
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

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
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

    public void setAccountAttributes(WebAuthnUserAccount account) {
        if (account != null) {
            this.emailAddress = account.getEmailAddress();
            this.confirmed = account.isConfirmed();
            String userHandle = account.getUserHandle();
            if (StringUtils.hasText(userHandle)) {
                attributes.put("userHandle", userHandle);
            }

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

        }
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void eraseCredentials() {
        // nothing to do
    }

}
