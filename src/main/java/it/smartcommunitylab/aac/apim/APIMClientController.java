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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.oauth.auth.DefaultOAuth2AuthenticatedPrincipal;
import springfox.documentation.annotations.ApiIgnore;

/*
 * Legacy integration with WSO2 apiManager 2.6
 * 
 * Relies on custom api and a dedicated client with 'clientmanagement' scope for a given realm.
 * Should be deprecated in favor of OAuth2 DCR
 */

@ApiIgnore
@Controller
public class APIMClientController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private APIMProviderService wso2Manager;

    @RequestMapping(value = "/wso2/client", method = RequestMethod.POST)
    @PreAuthorize("hasAuthority('" + Config.R_CLIENT + "') and hasAuthority('SCOPE_" + Config.SCOPE_CLIENTMANAGEMENT
            + "')")
    public @ResponseBody APIMClient createClient(@RequestBody APIMClient app, BearerTokenAuthentication auth)
            throws Exception {

        if (auth == null || !(auth.getPrincipal() instanceof DefaultOAuth2AuthenticatedPrincipal)) {
            logger.error("invalid authentication");
            throw new IllegalArgumentException("invalid authentication");
        }

        DefaultOAuth2AuthenticatedPrincipal principal = (DefaultOAuth2AuthenticatedPrincipal) auth.getPrincipal();
        String realm = principal.getRealm();

        String userName = app.getUserName();
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
        String[] scopes = app.getScope() != null ? app.getScope().split(APIMClient.SEPARATOR) : new String[0];
        String[] redirectUris = app.getRedirectUris() != null ? app.getRedirectUris().split(APIMClient.SEPARATOR)
                : new String[0];

        if (!StringUtils.hasText(userName) || !StringUtils.hasText(un) || !StringUtils.hasText(clientName)) {
            throw new InvalidDefinitionException("invalid parameters");
        }

        logger.trace("received create for " + app.toString());
        return wso2Manager.createClient(realm, clientId, un, clientName, displayName, clientSecret, grantTypes, scopes,
                redirectUris);

    }

    @RequestMapping(value = "/wso2/client/{clientId}", method = RequestMethod.PUT)
    @PreAuthorize("hasAuthority('" + Config.R_CLIENT + "') and hasAuthority('SCOPE_" + Config.SCOPE_CLIENTMANAGEMENT
            + "')")
    public @ResponseBody APIMClient updateClient(
            @PathVariable("clientId") String clientId,
            @RequestBody APIMClient app, BearerTokenAuthentication auth)
            throws Exception {

        if (auth == null || !(auth.getPrincipal() instanceof DefaultOAuth2AuthenticatedPrincipal)) {
            logger.error("invalid authentication");
            throw new IllegalArgumentException("invalid authentication");
        }

        DefaultOAuth2AuthenticatedPrincipal principal = (DefaultOAuth2AuthenticatedPrincipal) auth.getPrincipal();
        String realm = principal.getRealm();

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

        return wso2Manager.updateClient(realm, clientId, clientName, displayName, clientSecret, grantTypes, scopes,
                redirectUris);

    }

    @RequestMapping(value = "/wso2/client/{clientId}/validity/{validity}", method = RequestMethod.PATCH)
    @PreAuthorize("hasAuthority('" + Config.R_CLIENT + "') and hasAuthority('SCOPE_" + Config.SCOPE_CLIENTMANAGEMENT
            + "')")
    public @ResponseBody APIMClient updateTokenValidity(
            @PathVariable("clientId") String clientId, @PathVariable("validity") Integer validity,
            BearerTokenAuthentication auth) throws Exception {

        if (auth == null || !(auth.getPrincipal() instanceof DefaultOAuth2AuthenticatedPrincipal)) {
            logger.error("invalid authentication");
            throw new IllegalArgumentException("invalid authentication");
        }

        DefaultOAuth2AuthenticatedPrincipal principal = (DefaultOAuth2AuthenticatedPrincipal) auth.getPrincipal();
        String realm = principal.getRealm();

        return wso2Manager.updateValidity(realm, clientId, validity);

    }

    @RequestMapping(value = "/wso2/client/{clientId}/scope", method = RequestMethod.PUT)
    @PreAuthorize("hasAuthority('" + Config.R_CLIENT + "') and hasAuthority('SCOPE_" + Config.SCOPE_CLIENTMANAGEMENT
            + "')")
    public @ResponseBody APIMClient updateClientScope(
            @PathVariable("clientId") String clientId,
            @RequestParam String scope, BearerTokenAuthentication auth) throws Exception {

        if (auth == null || !(auth.getPrincipal() instanceof DefaultOAuth2AuthenticatedPrincipal)) {
            logger.error("invalid authentication");
            throw new IllegalArgumentException("invalid authentication");
        }

        DefaultOAuth2AuthenticatedPrincipal principal = (DefaultOAuth2AuthenticatedPrincipal) auth.getPrincipal();
        String realm = principal.getRealm();

        return wso2Manager.updateScope(realm, clientId, scope);

    }

    @RequestMapping(value = "/wso2/client/{clientId}", method = RequestMethod.GET)
    @PreAuthorize("hasAuthority('" + Config.R_CLIENT + "') and hasAuthority('SCOPE_" + Config.SCOPE_CLIENTMANAGEMENT
            + "')")
    public @ResponseBody APIMClient getClient(@PathVariable("clientId") String clientId, BearerTokenAuthentication auth)
            throws Exception {

        if (auth == null || !(auth.getPrincipal() instanceof DefaultOAuth2AuthenticatedPrincipal)) {
            logger.error("invalid authentication");
            throw new IllegalArgumentException("invalid authentication");
        }

        DefaultOAuth2AuthenticatedPrincipal principal = (DefaultOAuth2AuthenticatedPrincipal) auth.getPrincipal();
        String realm = principal.getRealm();

        return wso2Manager.getClient(realm, clientId);

    }

    @RequestMapping(value = "/wso2/client/{clientId}", method = RequestMethod.DELETE)
    @PreAuthorize("hasAuthority('" + Config.R_CLIENT + "') and hasAuthority('SCOPE_" + Config.SCOPE_CLIENTMANAGEMENT
            + "')")
    public @ResponseBody void deleteClient(@PathVariable("clientId") String clientId, BearerTokenAuthentication auth)
            throws Exception {

        if (auth == null || !(auth.getPrincipal() instanceof DefaultOAuth2AuthenticatedPrincipal)) {
            logger.error("invalid authentication");
            throw new IllegalArgumentException("invalid authentication");
        }

        DefaultOAuth2AuthenticatedPrincipal principal = (DefaultOAuth2AuthenticatedPrincipal) auth.getPrincipal();
        String realm = principal.getRealm();

        wso2Manager.deleteClient(realm, clientId);

    }

//    @RequestMapping("/wso2/token/revoke/{token}")
//    public @ResponseBody String revokeToken(@PathVariable String token) {
//        OAuth2AccessToken accessTokenObj = tokenStore.readAccessToken(token);
//        if (accessTokenObj != null) {
//            if (accessTokenObj.getRefreshToken() != null) {
//                tokenStore.removeRefreshToken(accessTokenObj.getRefreshToken());
//            }
//            tokenStore.removeAccessToken(accessTokenObj);
//        }
//        return "";
//    }

//    @RequestMapping(value = "/wso2/resources/{userName:.+}", method = RequestMethod.POST)
//    public @ResponseBody void createResources(HttpServletResponse response, @RequestBody AACService service,
//            @PathVariable("userName") String userName) throws Exception {
//        try {
//
//            String un = userName.replace("-AT-", "@");
//            String[] info = extractInfoFromTenant(un);
//
//            boolean ok = wso2Manager.createResource(service, info[0], info[1]);
//
//            if (!ok) {
//                response.setStatus(HttpStatus.BAD_REQUEST.value());
//            }
//
//        } catch (Exception e) {
//            logger.error(e.getMessage(), e);
//            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
//        }
//
//    }
//
//    @RequestMapping(value = "/wso2/resources/{resourceName:.+}", method = RequestMethod.DELETE)
//    public @ResponseBody void deleteResources(HttpServletResponse response,
//            @PathVariable("resourceName") String resourceName) throws Exception {
//        try {
//
//            String name = URLDecoder.decode(resourceName, "UTF-8");
//
//            wso2Manager.deleteResource(name);
//        } catch (Exception e) {
//            logger.error(e.getMessage(), e);
//            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
//        }
//
//    }

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

//    private String getUserNameAtTenant(String username, String tenantName) {
//        return username + "@" + tenantName;
//    }
//
//    private String[] extractInfoFromTenant(String tenant) {
//        int index = tenant.indexOf('@');
//        int lastIndex = tenant.lastIndexOf('@');
//
//        if (index != lastIndex) {
//            String result[] = new String[2];
//            result[0] = tenant.substring(0, lastIndex);
//            result[1] = tenant.substring(lastIndex + 1, tenant.length());
//            return result;
//        } else if (tenant.endsWith("@carbon.super")) {
//            return tenant.split("@");
//        }
//        return new String[] { tenant, "carbon.super" };
//    }

    @ExceptionHandler(InvalidDefinitionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Map<String, Object> invalid(InvalidDefinitionException e) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", "invalid_argument");
        error.put("error_description", e.getMessage());

        return error;
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
