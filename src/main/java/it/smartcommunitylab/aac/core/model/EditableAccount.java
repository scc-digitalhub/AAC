package it.smartcommunitylab.aac.core.model;

import java.util.Map;

import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

/*
 * An editable account has modifiable attributes and exposes a schema for UI
 */

public interface EditableAccount {

    public Map<String, String> getAttributes();

    public void setAttributes(Map<String, String> attributes);

    public JsonSchema getSchema();
}
