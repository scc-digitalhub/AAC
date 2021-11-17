package it.smartcommunitylab.aac.core.base;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.EditableAccount;

/*
 * An instantiable user account. 
 */

@JsonInclude(Include.NON_NULL)
public class DefaultAccountImpl extends BaseAccount implements EditableAccount {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    @JsonIgnore
    private String internalUserId;
    private String username;
    private String emailAddress;
    private Map<String, String> attributes = new HashMap<>();
    // jsonSchema describing attributes to serve UI
    private JsonSchema schema;

    public DefaultAccountImpl(String authority, String provider, String realm) {
        super(authority, provider, realm);
//        this.attributes = new HashMap<>();
    }

    public String getInternalUserId() {
        return internalUserId;
    }

    public void setInternalUserId(String internalUserId) {
        this.internalUserId = internalUserId;
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

    @Override
    public String getUserId() {
        if (userId == null) {
            // leverage the default mapper to translate internalId when missing
            return exportInternalId(internalUserId);
        } else {
            return userId;
        }
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
