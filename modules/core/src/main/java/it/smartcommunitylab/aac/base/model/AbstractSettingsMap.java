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

package it.smartcommunitylab.aac.base.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import it.smartcommunitylab.aac.accounts.provider.AccountServiceSettingsMap;
import it.smartcommunitylab.aac.attributes.provider.AttributeProviderSettingsMap;
import it.smartcommunitylab.aac.credentials.provider.CredentialsServiceSettingsMap;
import it.smartcommunitylab.aac.identity.provider.IdentityProviderSettingsMap;
import it.smartcommunitylab.aac.model.ConfigMap;
import it.smartcommunitylab.aac.repository.SchemaGeneratorFactory;
import it.smartcommunitylab.aac.templates.provider.TemplateProviderSettingsMap;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

//TODO evaluate adding generic type and resolving javatype for conversion here
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    {
        @Type(value = AccountServiceSettingsMap.class, name = AccountServiceSettingsMap.RESOURCE_TYPE),
        @Type(value = AttributeProviderSettingsMap.class, name = AttributeProviderSettingsMap.RESOURCE_TYPE),
        @Type(value = CredentialsServiceSettingsMap.class, name = CredentialsServiceSettingsMap.RESOURCE_TYPE),
        @Type(value = IdentityProviderSettingsMap.class, name = IdentityProviderSettingsMap.RESOURCE_TYPE),
        @Type(value = TemplateProviderSettingsMap.class, name = TemplateProviderSettingsMap.RESOURCE_TYPE),
    }
)
public abstract class AbstractSettingsMap implements ConfigMap, Serializable {

    protected static final ObjectMapper mapper = new ObjectMapper();
    private static final TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<
        HashMap<String, Serializable>
    >() {};
    protected static final JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(mapper);
    protected static final SchemaGenerator generator;

    static {
        generator = SchemaGeneratorFactory.build(mapper);
    }

    @JsonIgnore
    @Override
    public Map<String, Serializable> getConfiguration() {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        return mapper.convertValue(this, typeRef);
    }
}
