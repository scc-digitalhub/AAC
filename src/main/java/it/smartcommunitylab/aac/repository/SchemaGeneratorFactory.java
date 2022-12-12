package it.smartcommunitylab.aac.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
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
        ObjectMapper schemaMapper = new ObjectMapper()
                .setAnnotationIntrospector(new SchemaAnnotationIntrospector());
        return build(schemaMapper);
    }

    public static SchemaGenerator build(ObjectMapper schemaMapper) {
        JacksonModule jacksonModule = new JacksonModule(
                JacksonOption.IGNORE_TYPE_INFO_TRANSFORM,
                JacksonOption.RESPECT_JSONPROPERTY_ORDER);
        JavaxValidationModule javaxModule = new JavaxValidationModule(
                JavaxValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED,
                JavaxValidationOption.INCLUDE_PATTERN_EXPRESSIONS);
        Swagger2Module swagger2Module = new Swagger2Module();

        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(
                schemaMapper,
                SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .with(jacksonModule).with(javaxModule).with(swagger2Module)
                .without(Option.SCHEMA_VERSION_INDICATOR);
        SchemaGeneratorConfig config = configBuilder.build();
        return new SchemaGenerator(config);
    }
}
