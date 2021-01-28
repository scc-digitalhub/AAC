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

package it.smartcommunitylab.aac.apikey.endpoint;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.apikey.APIKey;
import it.smartcommunitylab.aac.apikey.APIKeyManager;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.model.Response;
import it.smartcommunitylab.aac.utils.SecurityUtils;

/**
 * @author raman
 *
 */
@Controller
@Api(tags = { "AAC Client ApiKey" })
public class APIKeyClientController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private APIKeyManager keyManager;

    /**
     * List client API keys
     * 
     * @return
     */
    @ApiOperation(value = "List client keys")
    @GetMapping(value = "/apikey/client/me")
    public @ResponseBody List<APIKey> getClientKeys(Authentication auth) {
        String clientId = SecurityUtils.getOAuthOrBasicClientId(auth, true);

        logger.trace("list keys for client " + clientId);
        // return keys basic info
        return keyManager.getClientKeys(clientId);
    }

    @ApiOperation(value = "Get key")
    @GetMapping(value = "/apikey/client/{apiKey:.*}")
    public @ResponseBody Map<String, Object> getKey(@PathVariable String apiKey,
            Authentication auth) throws EntityNotFoundException {
        String clientId = SecurityUtils.getOAuthOrBasicClientId(auth, true);
        Collection<String> scope = SecurityUtils.getOAuthScopes(auth);

        // fetch key and validate ownership
        APIKey key = keyManager.findKey(apiKey);
        if (key != null) {
            if (!scope.contains(Config.SCOPE_APIKEY_CLIENT_ALL) && !clientId.equals(key.getClientId())) {
                throw new SecurityException();
            }

            if (keyManager.isKeyValid(apiKey)) {

                // fetch a fully populated key
                APIKey apikey = keyManager.getKey(apiKey);

                // manually build a result
                Map<String, Object> json = APIKey.toMap(apikey);

                return json;
            }
        }

        // TODO evaluate returning a proper response instead of an error
        throw new EntityNotFoundException();
    }

    /**
     * Delete a specified API key
     * 
     * @param apiKey
     * @return
     */
    @ApiOperation(value = "Delete key")
    @DeleteMapping(value = "/apikey/client/{apiKey:.*}")
    public @ResponseBody void deleteKey(@PathVariable String apiKey,
            Authentication auth) throws SecurityException {
        String clientId = SecurityUtils.getOAuthOrBasicClientId(auth, true);
        Collection<String> scope = SecurityUtils.getOAuthScopes(auth);

        // fetch key and validate ownership
        APIKey key = keyManager.findKey(apiKey);
        if (key != null) {
            if (!scope.contains(Config.SCOPE_APIKEY_CLIENT_ALL) && !clientId.equals(key.getClientId())) {
                throw new SecurityException();
            }

            keyManager.deleteKey(apiKey);
        }
    }

    /**
     * Update a specified API key
     * 
     * @param apiKey
     * @return
     */
    @ApiOperation(value = "Update key")
    @PutMapping(value = "/apikey/client/{apiKey:.*}")
    public @ResponseBody APIKey updateKey(@PathVariable String apiKey,
            @RequestBody APIKey body,
            Authentication auth) throws SecurityException, EntityNotFoundException {
        String clientId = SecurityUtils.getOAuthOrBasicClientId(auth, true);
        Collection<String> scope = SecurityUtils.getOAuthScopes(auth);

        // fetch key and validate ownership
        APIKey key = keyManager.findKey(apiKey);
        if (key != null) {
            if (!scope.contains(Config.SCOPE_APIKEY_CLIENT_ALL) && !clientId.equals(key.getClientId())) {
                throw new SecurityException();
            }

            // client can only update its own information
            return keyManager.updateKeyData(apiKey, body.getAdditionalInformation());

        }

        throw new EntityNotFoundException();
    }

    /**
     * Create an API key with the specified properties (validity and additional
     * info)
     * 
     * @param apiKey
     * @return created entity
     */
    @ApiOperation(value = "Create key")
    @PostMapping(value = "/apikey/client")
    public @ResponseBody APIKey createKey(@RequestBody APIKey body,
            Authentication auth)
            throws EntityNotFoundException {
        String clientId = SecurityUtils.getOAuthOrBasicClientId(auth, true);
        Collection<String> scope = SecurityUtils.getOAuthScopes(auth);

        if (scope.contains(Config.SCOPE_APIKEY_CLIENT_ALL) && body.getClientId() != null) {
            // with manage all we can pass a specific client id
            clientId = body.getClientId();
        }

        int validity = 0;
        if (body.getValidity() != null) {
            validity = body.getValidity().intValue();
        }

        Set<String> scopes = null;
        if (body.getScope() != null) {
            scopes = new HashSet<>(Arrays.asList(body.getScope()));
        }

        // owner will be client developer
        return keyManager.createKey(clientId, null, validity, body.getAdditionalInformation(), scopes);
    }

    @ExceptionHandler(InvalidDefinitionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Response processDefinitionError(InvalidDefinitionException ex) {
        return Response.error(ex.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Key or client does not exist")
    public void notFound() {
    }

    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(code = HttpStatus.UNAUTHORIZED, reason = "Operation not permitted")
    public void unauthorized() {
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
    @ResponseStatus(code = HttpStatus.UNAUTHORIZED, reason = "Operation not permitted")
    @ResponseBody
    public void authError(InsufficientAuthenticationException ex) {
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public void handleBadRequest(HttpServletRequest req, Exception ex) {
        logger.error("Error processing API Key operation", ex);
    }

}
