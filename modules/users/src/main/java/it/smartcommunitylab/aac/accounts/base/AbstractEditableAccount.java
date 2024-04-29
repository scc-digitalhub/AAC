/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.accounts.base;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import it.smartcommunitylab.aac.accounts.model.EditableUserAccount;
import it.smartcommunitylab.aac.base.model.AbstractBaseResource;
import it.smartcommunitylab.aac.repository.JsonSchemaIgnore;
import it.smartcommunitylab.aac.repository.SchemaAnnotationIntrospector;
import it.smartcommunitylab.aac.repository.SchemaGeneratorFactory;
import it.smartcommunitylab.aac.users.base.AbstractBaseUserResource;

/*
 * Abstract class for editable user accounts
 *
 * all implementations should derive from this
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
// @JsonSubTypes(
//     {
//         @Type(value = InternalEditableUserAccount.class, name = InternalEditableUserAccount.RESOURCE_TYPE),
//         @Type(value = OIDCEditableUserAccount.class, name = OIDCEditableUserAccount.RESOURCE_TYPE),
//         @Type(value = SamlEditableUserAccount.class, name = SamlEditableUserAccount.RESOURCE_TYPE),
//     }
// )
public abstract class AbstractEditableAccount extends AbstractBaseUserResource implements EditableUserAccount {

    protected static final SchemaGenerator generator;

    static {
        ObjectMapper schemaMapper = new ObjectMapper()
            .setAnnotationIntrospector(
                new SchemaAnnotationIntrospector(
                    AbstractEditableAccount.class,
                    AbstractBaseUserResource.class,
                    AbstractBaseResource.class
                )
            );
        generator = SchemaGeneratorFactory.build(schemaMapper);
    }

    protected AbstractEditableAccount(String authority, String provider, String realm, String id) {
        super(authority, provider, realm, id, null);
    }

    protected AbstractEditableAccount(String authority, String provider, String realm, String id, String userId) {
        super(authority, provider, realm, id, userId);
    }

    @Override
    @JsonSchemaIgnore
    public abstract String getType();

    @Override
    @JsonSchemaIgnore
    public abstract String getAccountId();

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
