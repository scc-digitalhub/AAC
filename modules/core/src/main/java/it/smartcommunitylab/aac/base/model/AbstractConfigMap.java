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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import io.swagger.v3.oas.annotations.media.Schema;
import it.smartcommunitylab.aac.model.ConfigMap;
import it.smartcommunitylab.aac.repository.SchemaGeneratorFactory;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;

//TODO evaluate adding generic type and resolving javatype for conversion here
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
// @JsonSubTypes(
//     {
//         @Type(value = InternalIdentityProviderConfigMap.class, name = InternalIdentityProviderConfigMap.RESOURCE_TYPE),
//         @Type(value = PasswordIdentityProviderConfigMap.class, name = PasswordIdentityProviderConfigMap.RESOURCE_TYPE),
//         @Type(value = WebAuthnIdentityProviderConfigMap.class, name = WebAuthnIdentityProviderConfigMap.RESOURCE_TYPE),
//         @Type(value = AppleIdentityProviderConfigMap.class, name = AppleIdentityProviderConfigMap.RESOURCE_TYPE),
//         @Type(value = OIDCIdentityProviderConfigMap.class, name = OIDCIdentityProviderConfigMap.RESOURCE_TYPE),
//         @Type(value = SamlIdentityProviderConfigMap.class, name = SamlIdentityProviderConfigMap.RESOURCE_TYPE),
//         @Type(value = MapperAttributeProviderConfigMap.class, name = MapperAttributeProviderConfigMap.RESOURCE_TYPE),
//         @Type(value = ScriptAttributeProviderConfigMap.class, name = ScriptAttributeProviderConfigMap.RESOURCE_TYPE),
//         @Type(value = WebhookAttributeProviderConfigMap.class, name = WebhookAttributeProviderConfigMap.RESOURCE_TYPE),
//         @Type(value = TemplateProviderConfigMap.class, name = TemplateProviderConfigMap.RESOURCE_TYPE),
//     }
// )
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(title = "configuration")
public abstract class AbstractConfigMap implements ConfigMap, Serializable, ResolvableTypeProvider {

    protected static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<
        HashMap<String, Serializable>
    >() {};
    protected static final SchemaGenerator generator;
    protected transient JsonNode schema;
    private ResolvableType ctype;

    static {
        generator = SchemaGeneratorFactory.GENERATOR;
    }

    protected AbstractConfigMap() {}

    @Override
    @JsonIgnore
    public JsonNode getSchema() throws JsonMappingException {
        if (schema == null) {
            schema = generator.generateSchema(getClass());
        }

        return schema;
    }

    @JsonIgnore
    @Override
    public Map<String, Serializable> getConfiguration() {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        return mapper.convertValue(this, typeRef);
    }

    @Override
    @JsonIgnore
    public ResolvableType getResolvableType() {
        if (this.ctype == null) {
            try {
                this.ctype = ResolvableType.forClass(getClass());
            } catch (Exception e) {
                //ignore
            }
        }

        return ctype;
    }
}
