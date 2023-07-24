package it.smartcommunitylab.aac.core.base;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import it.smartcommunitylab.aac.core.model.EditableUserAccount;
import it.smartcommunitylab.aac.internal.model.InternalEditableUserAccount;
import it.smartcommunitylab.aac.openid.model.OIDCEditableUserAccount;
import it.smartcommunitylab.aac.repository.JsonSchemaIgnore;
import it.smartcommunitylab.aac.repository.SchemaAnnotationIntrospector;
import it.smartcommunitylab.aac.repository.SchemaGeneratorFactory;
import it.smartcommunitylab.aac.saml.model.SamlEditableUserAccount;

/*
 * Abstract class for editable user accounts
 *
 * all implementations should derive from this
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes(
    {
        @Type(value = InternalEditableUserAccount.class, name = InternalEditableUserAccount.RESOURCE_TYPE),
        @Type(value = OIDCEditableUserAccount.class, name = OIDCEditableUserAccount.RESOURCE_TYPE),
        @Type(value = SamlEditableUserAccount.class, name = SamlEditableUserAccount.RESOURCE_TYPE),
    }
)
public abstract class AbstractEditableAccount extends AbstractBaseUserResource implements EditableUserAccount {

    protected static final SchemaGenerator generator;

    static {
        ObjectMapper schemaMapper = new ObjectMapper()
            .setAnnotationIntrospector(
                new SchemaAnnotationIntrospector(AbstractEditableAccount.class, AbstractBaseUserResource.class)
            );
        generator = SchemaGeneratorFactory.build(schemaMapper);
    }

    protected String uuid;
    protected String userId;
    protected String realm;

    protected AbstractEditableAccount(String authority, String provider, String uuid) {
        super(authority, provider);
        this.uuid = uuid;
    }

    @Override
    public String getId() {
        // use uuid from editable model
        return getUuid();
    }

    @Override
    public String getResourceId() {
        return getAccountId();
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
    public abstract String getAccountId();
}
