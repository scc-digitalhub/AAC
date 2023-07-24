package it.smartcommunitylab.aac.core.model;

import com.fasterxml.jackson.databind.JsonNode;
import it.smartcommunitylab.aac.repository.JsonSchemaIgnore;

public interface EditableResource extends Resource {
    @JsonSchemaIgnore
    public JsonNode getSchema();
}
