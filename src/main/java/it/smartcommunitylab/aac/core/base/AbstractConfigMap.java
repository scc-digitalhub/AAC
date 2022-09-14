package it.smartcommunitylab.aac.core.base;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;

import it.smartcommunitylab.aac.core.model.ConfigMap;

public abstract class AbstractConfigMap implements ConfigMap {
    protected static ObjectMapper mapper = new ObjectMapper();
    private final static TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<HashMap<String, Serializable>>() {
    };
    protected static final JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(mapper);

    @JsonIgnore
    @Override
    public Map<String, Serializable> getConfiguration() {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        return mapper.convertValue(this, typeRef);
    }

}