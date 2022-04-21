package it.smartcommunitylab.aac.core.base;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.model.UserStatus;

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
    // jsonSchema describing attributes to serve UI
    private JsonSchema schema;

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
        return UserStatus.LOCKED.getValue().equals(status);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @JsonIgnore
    public JsonSchema getSchema() {
        if (schema != null) {
            return schema;
        } else {
            // describe attributesMap as strings, at minimum we enumerate properties
            // TODO manual build of jsonschema, or drop map<> in place of
            // configurableProperties obj
            return null;
        }
    }

    public void setSchema(JsonSchema schema) {
        this.schema = schema;
    }

}
