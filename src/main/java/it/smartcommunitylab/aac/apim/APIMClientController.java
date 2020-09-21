/**
 *    Copyright 2015-2019 Smart Community Lab, FBK
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
 */

package it.smartcommunitylab.aac.apim;

import java.net.URLDecoder;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.Utils;
import springfox.documentation.annotations.ApiIgnore;

@ApiIgnore
@Controller
public class APIMClientController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private APIMProviderService wso2Manager;

    @Autowired
    private TokenStore tokenStore;

    // custom mapper, TODO check if needed
    private static final ObjectMapper mapper = new ObjectMapper().configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @RequestMapping(value = "/wso2/client/{userName:.+}", method = RequestMethod.POST)
    public @ResponseBody APIMClient createClient(HttpServletResponse response, @RequestBody APIMClient app,
            @PathVariable("userName") String userName) throws Exception {

        String un = extractUserFromTenant(userName);

        // extract data
        String clientId = app.getClientId();
        String clientName = app.getName();
        String displayName = app.getDisplayName();
        String clientSecret = app.getClientSecret();
        Collection<String> grantTypes = app.getGrantedTypes();
        if (grantTypes == null || grantTypes.isEmpty()) {
            // always assign CLIENT_CREDENTIALS
            grantTypes = Collections.singleton(Config.GRANT_TYPE_CLIENT_CREDENTIALS);
        }
        String[] scopes = app.getScope() != null ? app.getScope().split(APIMClient.SEPARATOR) : null;
        String[] redirectUris = app.getRedirectUris() != null ? app.getRedirectUris().split(APIMClient.SEPARATOR)
                : null;

        return wso2Manager.createClient(clientId, un, clientName, displayName, clientSecret, grantTypes, scopes,
                redirectUris);

    }

    @RequestMapping(value = "/wso2/client/{clientId}", method = RequestMethod.PUT)
    public @ResponseBody APIMClient updateClient(HttpServletResponse response, @RequestBody APIMClient app,
            @PathVariable("clientId") String clientId) throws Exception {

        // extract data
        String clientName = app.getName();
        String displayName = app.getDisplayName();
        String clientSecret = app.getClientSecret();
        Collection<String> grantTypes = app.getGrantedTypes();
        if (grantTypes == null || grantTypes.isEmpty()) {
            // always assign CLIENT_CREDENTIALS
            grantTypes = Collections.singleton(Config.GRANT_TYPE_CLIENT_CREDENTIALS);
        }
        String[] scopes = app.getScope() != null ? app.getScope().split(APIMClient.SEPARATOR) : null;
        String[] redirectUris = app.getRedirectUris() != null ? app.getRedirectUris().split(APIMClient.SEPARATOR)
                : null;

        return wso2Manager.updateClient(clientId, clientName, displayName, clientSecret, grantTypes, scopes,
                redirectUris);

    }

    @RequestMapping(value = "/wso2/client/validity/{clientId}/{validity}", method = RequestMethod.PATCH)
    public @ResponseBody APIMClient updateTokenValidity(HttpServletResponse response,
            @PathVariable("clientId") String clientId, @PathVariable("validity") Integer validity) throws Exception {

        return wso2Manager.updateValidity(clientId, validity);

    }

    @RequestMapping(value = "/wso2/client/scope/{clientId}", method = RequestMethod.POST)
    public @ResponseBody APIMClient updateClientScope(HttpServletResponse response,
            @PathVariable("clientId") String clientId,
            @RequestParam String scope) throws Exception {

        return wso2Manager.updateScope(clientId, scope);

    }

    @RequestMapping(value = "/wso2/client/{clientId}", method = RequestMethod.GET)
    public @ResponseBody APIMClient getClient(HttpServletResponse response, @PathVariable("clientId") String clientId)
            throws Exception {

        return wso2Manager.getClient(clientId);

    }

    @RequestMapping(value = "/wso2/client/{clientId}", method = RequestMethod.DELETE)
    public @ResponseBody void deleteClient(HttpServletResponse response, @PathVariable("clientId") String clientId)
            throws Exception {

        wso2Manager.deleteClient(clientId);

    }

    @RequestMapping("/wso2/client/token_revoke/{token}")
    public @ResponseBody String revokeToken(@PathVariable String token) {
        OAuth2AccessToken accessTokenObj = tokenStore.readAccessToken(token);
        if (accessTokenObj != null) {
            if (accessTokenObj.getRefreshToken() != null) {
                tokenStore.removeRefreshToken(accessTokenObj.getRefreshToken());
            }
            tokenStore.removeAccessToken(accessTokenObj);
        }
        return "";
    }

    @RequestMapping(value = "/wso2/resources/{userName:.+}", method = RequestMethod.POST)
    public @ResponseBody void createResources(HttpServletResponse response, @RequestBody AACService service,
            @PathVariable("userName") String userName) throws Exception {
        try {

            String un = userName.replace("-AT-", "@");
            String[] info = extractInfoFromTenant(un);

            boolean ok = wso2Manager.createResource(service, info[0], info[1]);

            if (!ok) {
                response.setStatus(HttpStatus.BAD_REQUEST.value());
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

    }

    @RequestMapping(value = "/wso2/resources/{resourceName:.+}", method = RequestMethod.DELETE)
    public @ResponseBody void deleteResources(HttpServletResponse response,
            @PathVariable("resourceName") String resourceName) throws Exception {
        try {

            String name = URLDecoder.decode(resourceName, "UTF-8");

            wso2Manager.deleteResource(name);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

    }

    /*
     * Helpers - TODO cleanup
     */

    private String extractUserFromTenant(String tenant) {
        String un = tenant;

        int index = un.indexOf('@');
        int lastIndex = un.lastIndexOf('@');

        if (index != lastIndex) {
            un = un.substring(0, lastIndex);
        } else if (un.endsWith("@carbon.super")) {
            un = un.substring(0, un.indexOf('@'));
        }

        return un;
    }

    private String getUserNameAtTenant(String username, String tenantName) {
        return username + "@" + tenantName;
    }

    private String[] extractInfoFromTenant(String tenant) {
        int index = tenant.indexOf('@');
        int lastIndex = tenant.lastIndexOf('@');

        if (index != lastIndex) {
            String result[] = new String[2];
            result[0] = tenant.substring(0, lastIndex);
            result[1] = tenant.substring(lastIndex + 1, tenant.length());
            return result;
        } else if (tenant.endsWith("@carbon.super")) {
            return tenant.split("@");
        }
        return new String[] { tenant, "carbon.super" };
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public Map<String, Object> noSuchEntity(EntityNotFoundException e) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", "entity_not_found");
        error.put("error_description", e.getMessage());

        return error;
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public Map<String, Object> systemException(RuntimeException e) {
        logger.error(e.getMessage(), e);
        Map<String, Object> error = new HashMap<>();
        error.put("error", "system_error");
        error.put("error_description", e.getMessage());

        return error;
    }

}
