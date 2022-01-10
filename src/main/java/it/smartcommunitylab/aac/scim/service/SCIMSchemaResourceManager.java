/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.aac.scim.service;

import static org.wso2.charon3.core.schema.SCIMConstants.GROUP;
import static org.wso2.charon3.core.schema.SCIMConstants.GROUP_CORE_SCHEMA_URI;
import static org.wso2.charon3.core.schema.SCIMConstants.USER;
import static org.wso2.charon3.core.schema.SCIMConstants.USER_CORE_SCHEMA_URI;
import static org.wso2.charon3.core.schema.SCIMConstants.ResourceTypeSchemaConstants.USER_ACCOUNT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.charon3.core.encoder.JSONEncoder;
import org.wso2.charon3.core.exceptions.BadRequestException;
import org.wso2.charon3.core.exceptions.CharonException;
import org.wso2.charon3.core.exceptions.NotFoundException;
import org.wso2.charon3.core.protocol.ResponseCodeConstants;
import org.wso2.charon3.core.protocol.SCIMResponse;
import org.wso2.charon3.core.protocol.endpoints.AbstractResourceManager;
import org.wso2.charon3.core.schema.AttributeSchema;
import org.wso2.charon3.core.schema.SCIMConstants;
import org.wso2.charon3.core.schema.SCIMDefinitions.DataType;
import org.wso2.charon3.core.schema.SCIMDefinitions.Mutability;
import org.wso2.charon3.core.schema.SCIMSchemaDefinitions;
/**
 * @author raman
 *
 */
public class SCIMSchemaResourceManager {

    private static JSONEncoder encoder = new JSONEncoder();


    private static final String ATTRIBUTES = "attributes";

    /*
     * Returns the encoder for json.
     *
     * @return JSONEncoder - An json encoder for encoding data
     * @throws CharonException
     */
    public static JSONEncoder getEncoder() throws CharonException {
        return encoder;
    }

    public SCIMResponse buildSchemaResponse() throws CharonException {
    	try {
			JSONArray rootObject = new JSONArray();
			JSONObject userSchemaObject = buildUserSchema();
			rootObject.put(userSchemaObject);
			JSONObject groupSchemaObject = buildGroupSchema();
			rootObject.put(groupSchemaObject);
			
			Map<String, String> responseHeaders = getResponseHeaders();
			return new SCIMResponse(ResponseCodeConstants.CODE_OK, rootObject.toString(), responseHeaders);
		} catch (BadRequestException | NotFoundException e) {
			throw new CharonException(e.getMessage());
		}
    }
    
    public SCIMResponse buildSchemaResponse(String type) throws CharonException {
    	JSONObject schemaObject;
		Map<String, String> responseHeaders;
		try {
			schemaObject = null;
			if (USER_CORE_SCHEMA_URI.equals(type)) {
				schemaObject = buildUserSchema();
			} else if (GROUP_CORE_SCHEMA_URI.equals(type)) {
				schemaObject = buildGroupSchema();
			} else {
				throw new CharonException("Unsupported schema");
			}
			responseHeaders = getResponseHeaders();
			return new SCIMResponse(ResponseCodeConstants.CODE_OK, schemaObject.toString(), responseHeaders);
		} catch (BadRequestException | NotFoundException e) {
			throw new CharonException(e.getMessage());
		}
    }
    
    private Map<String, String> getResponseHeaders() throws NotFoundException {

        Map<String, String> responseHeaders;
        responseHeaders = new HashMap<>();
        responseHeaders.put(SCIMConstants.CONTENT_TYPE_HEADER, SCIMConstants.APPLICATION_JSON);
        responseHeaders.put(SCIMConstants.LOCATION_HEADER,  AbstractResourceManager.getResourceEndpointURL(SCIMConstants.SCHEMAS_ENDPOINT));
        return responseHeaders;
    }
    
    private JSONObject buildUserSchema() throws CharonException, BadRequestException {

        try {
            JSONEncoder encoder = getEncoder();

            JSONObject coreSchemaObject = new JSONObject();
            coreSchemaObject.put(SCIMConstants.CommonSchemaConstants.ID, USER_CORE_SCHEMA_URI);
            coreSchemaObject.put(SCIMConstants.UserSchemaConstants.NAME, USER);
            coreSchemaObject.put(SCIMConstants.ResourceTypeSchemaConstants.DESCRIPTION, USER_ACCOUNT);
            
            List<AttributeSchema> attributeSchemas = SCIMSchemaDefinitions.SCIM_USER_SCHEMA.getAttributesList();
            
            JSONArray coreSchemaAttributeArray = buildSchemaAttributeArray(attributeSchemas, encoder);
            coreSchemaObject.put(ATTRIBUTES, coreSchemaAttributeArray);
            return coreSchemaObject;
        } catch (JSONException e) {
            throw new CharonException("Error while encoding core schema.", e);
        }
    }

    private JSONObject buildGroupSchema() throws CharonException, BadRequestException {

        try {
            JSONEncoder encoder = getEncoder();

            JSONObject coreSchemaObject = new JSONObject();
            coreSchemaObject.put(SCIMConstants.CommonSchemaConstants.ID, GROUP_CORE_SCHEMA_URI);
            coreSchemaObject.put(SCIMConstants.UserSchemaConstants.NAME, GROUP);
            coreSchemaObject.put(SCIMConstants.ResourceTypeSchemaConstants.DESCRIPTION, GROUP);
            
            List<AttributeSchema> attributeSchemas = SCIMSchemaDefinitions.SCIM_GROUP_SCHEMA.getAttributesList();
            JSONArray coreSchemaAttributeArray = buildSchemaAttributeArray(attributeSchemas, encoder);
            coreSchemaObject.put(ATTRIBUTES, coreSchemaAttributeArray);
            return coreSchemaObject;
        } catch (JSONException e) {
            throw new CharonException("Error while encoding core schema.", e);
        }
    }
    
    private JSONArray buildSchemaAttributeArray(List<AttributeSchema> attributeSchemas, JSONEncoder encoder)
            throws JSONException {

        JSONArray schemaAttributeArray = new JSONArray();

        for (AttributeSchema schemaAttribute : attributeSchemas) {

            JSONObject schemaJSONAttribute = schemaAttributeToJSON(schemaAttribute);
            schemaAttributeArray.put(schemaJSONAttribute);
        }

        return schemaAttributeArray;
    }

	private JSONObject schemaAttributeToJSON(AttributeSchema sa) {
		JSONObject res = new JSONObject();
		res.put("uri", sa.getURI());
		res.put("name", sa.getName());
		res.put("type", convertType(sa.getType()));
		res.put("multiValued", sa.getMultiValued());
		res.put("description", sa.getDescription());
		res.put("caseExact", sa.getCaseExact());
		res.put("mutability", convertMutability(sa.getMutability()));
		res.put("returned", sa.getReturned().name().toLowerCase());
		res.put("uniqueness", sa.getUniqueness().name().toLowerCase());
		res.put("required", sa.getRequired());
		
		if (sa.getSubAttributeSchemas() != null && sa.getSubAttributeSchemas().size() > 0) {
			JSONArray arr = new JSONArray();
			for (AttributeSchema sub: sa.getSubAttributeSchemas()) {
				arr.put(schemaAttributeToJSON(sub));
			}
			res.put("subAttributes", arr);
		}
		return res;
	}

	/**
	 * @param type
	 * @return
	 */
	private String convertType(DataType type) {
		return type.equals(DataType.DATE_TIME) ? "dateTime" : type.name().toLowerCase();
	}

	/**
	 * @param mutability
	 * @return
	 */
	private String convertMutability(Mutability mutability) {
		return mutability.equals(Mutability.READ_ONLY) ? "readOnly" : mutability.equals(Mutability.READ_WRITE) ? "readWrite" : mutability.name().toLowerCase();
	}

}
