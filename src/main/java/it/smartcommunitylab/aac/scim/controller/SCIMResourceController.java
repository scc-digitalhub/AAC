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

package it.smartcommunitylab.aac.scim.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.wso2.charon3.core.exceptions.CharonException;
import org.wso2.charon3.core.protocol.SCIMResponse;

/**
 * @author raman
 *
 */
public class SCIMResourceController {

	
	public ResponseEntity<?> buildResponse(String realm, SCIMResponse scimResponse) throws CharonException {
        //create a response builder with the status code of the response to be returned.
		ResponseEntity.BodyBuilder builder = ResponseEntity.status(scimResponse.getResponseStatus());
		
        //set the headers on the response
        Map<String, String> httpHeaders = scimResponse.getHeaderParamMap();
        if (httpHeaders != null && !httpHeaders.isEmpty()) {
            for (Map.Entry<String, String> entry : httpHeaders.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
        }
        //set the payload of the response, if available.
        if (scimResponse.getResponseMessage() != null) {
            return builder.body(scimResponse.getResponseMessage());
        }
        return builder.build();
    }
}
