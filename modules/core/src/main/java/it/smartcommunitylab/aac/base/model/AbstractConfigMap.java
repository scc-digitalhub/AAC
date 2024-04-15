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
import it.smartcommunitylab.aac.attributes.provider.MapperAttributeProviderConfigMap;
import it.smartcommunitylab.aac.attributes.provider.ScriptAttributeProviderConfigMap;
import it.smartcommunitylab.aac.attributes.provider.WebhookAttributeProviderConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfigMap;
import it.smartcommunitylab.aac.oidc.apple.provider.AppleIdentityProviderConfigMap;
import it.smartcommunitylab.aac.oidc.provider.OIDCIdentityProviderConfigMap;
import it.smartcommunitylab.aac.password.provider.PasswordIdentityProviderConfigMap;
import it.smartcommunitylab.aac.repository.SchemaGeneratorFactory;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProviderConfigMap;
import it.smartcommunitylab.aac.templates.provider.TemplateProviderConfigMap;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfigMap;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

//TODO evaluate adding generic type and resolving javatype for conversion here
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    {
        @Type(value = InternalIdentityProviderConfigMap.class, name = InternalIdentityProviderConfigMap.RESOURCE_TYPE),
        @Type(value = PasswordIdentityProviderConfigMap.class, name = PasswordIdentityProviderConfigMap.RESOURCE_TYPE),
        @Type(value = WebAuthnIdentityProviderConfigMap.class, name = WebAuthnIdentityProviderConfigMap.RESOURCE_TYPE),
        @Type(value = AppleIdentityProviderConfigMap.class, name = AppleIdentityProviderConfigMap.RESOURCE_TYPE),
        @Type(value = OIDCIdentityProviderConfigMap.class, name = OIDCIdentityProviderConfigMap.RESOURCE_TYPE),
        @Type(value = SamlIdentityProviderConfigMap.class, name = SamlIdentityProviderConfigMap.RESOURCE_TYPE),
        @Type(value = MapperAttributeProviderConfigMap.class, name = MapperAttributeProviderConfigMap.RESOURCE_TYPE),
        @Type(value = ScriptAttributeProviderConfigMap.class, name = ScriptAttributeProviderConfigMap.RESOURCE_TYPE),
        @Type(value = WebhookAttributeProviderConfigMap.class, name = WebhookAttributeProviderConfigMap.RESOURCE_TYPE),
        @Type(value = TemplateProviderConfigMap.class, name = TemplateProviderConfigMap.RESOURCE_TYPE),
    }
)
public abstract class AbstractConfigMap implements ConfigMap, Serializable {

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
