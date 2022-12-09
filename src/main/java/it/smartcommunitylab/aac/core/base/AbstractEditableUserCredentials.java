package it.smartcommunitylab.aac.core.base;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import it.smartcommunitylab.aac.core.model.EditableUserCredentials;
import it.smartcommunitylab.aac.password.dto.InternalEditableUserPassword;
import it.smartcommunitylab.aac.repository.JsonSchemaIgnore;
import it.smartcommunitylab.aac.repository.SchemaAnnotationIntrospector;
import it.smartcommunitylab.aac.repository.SchemaGeneratorFactory;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnEditableUserCredential;

/*
 * Abstract class for editable user credentials
 * 
 * all implementations should derive from this
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
        @Type(value = WebAuthnEditableUserCredential.class, name = WebAuthnEditableUserCredential.RESOURCE_TYPE),
        @Type(value = InternalEditableUserPassword.class, name = InternalEditableUserPassword.RESOURCE_TYPE)
})
public abstract class AbstractEditableUserCredentials extends AbstractBaseUserResource
        implements EditableUserCredentials {

    protected final static SchemaGenerator generator;

    static {
        ObjectMapper schemaMapper = new ObjectMapper()
                .setAnnotationIntrospector(new SchemaAnnotationIntrospector(
                        AbstractEditableUserCredentials.class, AbstractBaseUserResource.class));
        generator = SchemaGeneratorFactory.build(schemaMapper);
    }

    protected String uuid;
    protected String userId;
    protected String realm;

    protected AbstractEditableUserCredentials(String authority, String provider, String uuid) {
        super(authority, provider);
        this.uuid = uuid;
    }

    @Override
    public String getId() {
        // use uuid from persisted model
        return getUuid();
    }

    @Override
    public String getResourceId() {
        return getCredentialsId();
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    @Override
    @JsonSchemaIgnore
    public abstract String getType();

    @Override
    @JsonSchemaIgnore
    public abstract String getCredentialsId();
}
