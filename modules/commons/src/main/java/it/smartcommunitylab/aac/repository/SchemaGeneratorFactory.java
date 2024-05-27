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

package it.smartcommunitylab.aac.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.javax.validation.JavaxValidationModule;
import com.github.victools.jsonschema.module.javax.validation.JavaxValidationOption;
import com.github.victools.jsonschema.module.swagger2.Swagger2Module;

public class SchemaGeneratorFactory {

    public static SchemaGenerator build() {
        ObjectMapper schemaMapper = new ObjectMapper().setAnnotationIntrospector(new SchemaAnnotationIntrospector());
        return build(schemaMapper);
    }

    public static SchemaGenerator build(ObjectMapper schemaMapper) {
        JacksonModule jacksonModule = new JacksonModule(
            JacksonOption.IGNORE_TYPE_INFO_TRANSFORM,
            JacksonOption.RESPECT_JSONPROPERTY_ORDER
        );
        JavaxValidationModule javaxModule = new JavaxValidationModule(
            JavaxValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED,
            JavaxValidationOption.INCLUDE_PATTERN_EXPRESSIONS
        );
        Swagger2Module swagger2Module = new Swagger2Module();

        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(
            schemaMapper,
            SchemaVersion.DRAFT_2020_12,
            OptionPreset.PLAIN_JSON
        )
            .with(jacksonModule)
            .with(javaxModule)
            .with(swagger2Module)
            .without(Option.SCHEMA_VERSION_INDICATOR);
        SchemaGeneratorConfig config = configBuilder.build();
        return new SchemaGenerator(config);
    }

    public static final SchemaGenerator GENERATOR;

    static {
        ObjectMapper schemaMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setAnnotationIntrospector(new SchemaAnnotationIntrospector());

        JacksonModule jacksonModule = new JacksonModule(
            JacksonOption.IGNORE_TYPE_INFO_TRANSFORM,
            JacksonOption.RESPECT_JSONPROPERTY_ORDER,
            JacksonOption.INCLUDE_ONLY_JSONPROPERTY_ANNOTATED_METHODS
        );
        // JakartaValidationModule jakartaModule = new JakartaValidationModule(
        //     JakartaValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED,
        //     JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS
        // );
        Swagger2Module swagger2Module = new Swagger2Module();

        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(
            schemaMapper,
            SchemaVersion.DRAFT_2020_12,
            OptionPreset.PLAIN_JSON
        )
            .with(jacksonModule)
            // .with(jakartaModule)
            .with(swagger2Module)
            //options
            .with(Option.DEFINITIONS_FOR_ALL_OBJECTS)
            .with(Option.MAP_VALUES_AS_ADDITIONAL_PROPERTIES)
            .with(Option.PLAIN_DEFINITION_KEYS)
            .with(Option.ENUM_KEYWORD_FOR_SINGLE_VALUES)
            .with(Option.FLATTENED_ENUMS_FROM_TOSTRING)
            //avoid fields without getters (ex. unwrapped fields)
            .without(Option.NONPUBLIC_NONSTATIC_FIELDS_WITHOUT_GETTERS);

        GENERATOR = new SchemaGenerator(configBuilder.build());
    }

    public static JsonNode schema(Class<?> clazz) {
        return GENERATOR.generateSchema(clazz);
    }

    private SchemaGeneratorFactory() {}
}
