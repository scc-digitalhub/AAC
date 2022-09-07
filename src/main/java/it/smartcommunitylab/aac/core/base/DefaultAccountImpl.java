package it.smartcommunitylab.aac.core.base;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.model.SubjectStatus;

/*
 * An instantiable user account. 
 */

@JsonInclude(Include.NON_NULL)
public class DefaultAccountImpl extends AbstractAccount {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private String id;
    private String uuid;
    private String username;
    private String emailAddress;
    private Boolean emailVerified;
    private String status;
    private Map<String, String> attributes = new HashMap<>();

    public DefaultAccountImpl(String authority, String provider, String realm) {
        super(authority, provider, realm);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public boolean isEmailVerified() {
        return emailVerified != null ? emailVerified.booleanValue() : false;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public void addAttribute(String key, String value) {
        this.attributes.put(key, value);
    }

    public String getAttribute(String key) {
        return this.attributes.get(key);
    }

    @Override
    public boolean isLocked() {
        return SubjectStatus.LOCKED.getValue().equals(status);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
