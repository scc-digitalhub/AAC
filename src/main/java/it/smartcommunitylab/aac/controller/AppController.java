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

package it.smartcommunitylab.aac.controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.apikey.manager.APIKeyManager;
import it.smartcommunitylab.aac.bootstrap.BootstrapClient;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.YamlUtils;
import it.smartcommunitylab.aac.dto.APIKey;
import it.smartcommunitylab.aac.manager.ClaimManager;
import it.smartcommunitylab.aac.manager.ClientDetailsManager;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.ClientAppBasic;
import it.smartcommunitylab.aac.model.ClientAppInfo;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.Response;
import it.smartcommunitylab.aac.model.Response.RESPONSE;
import it.smartcommunitylab.aac.model.Role;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Controller for performing the basic operations over the client apps.
 * 
 * @author raman
 *
 */
@ApiIgnore
@Controller
public class AppController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ClientDetailsManager clientDetailsAdapter;
    @Autowired
    private ClientDetailsRepository clientDetailsRepo;
    @Autowired
    private UserManager userManager;
    @Autowired
    private APIKeyManager keyManager;
    @Autowired
    private ClaimManager claimManager;

    /**
     * Retrieve the with the user data: currently on the username is added.
     * 
     * @return
     */
    @RequestMapping("/")
    public ModelAndView home() {
        return new ModelAndView("redirect:/account");
    }

    /**
     * Retrieve the with the user data: currently on the username is added.
     * 
     * @return
     */
    @RequestMapping("/dev")
    public ModelAndView developer() {
        Map<String, Object> model = new HashMap<String, Object>();

        String username = userManager.getUserFullName();
        model.put("username", username);
        Set<String> userRoles = userManager.getUserRoles();
        model.put("roles", userRoles);
        model.put("contexts",
                userManager.getUser().getRoles().stream().filter(r -> r.getRole().equals(Config.R_PROVIDER))
                        .map(Role::canonicalSpace).collect(Collectors.toSet()));
        String check = ":" + Config.R_PROVIDER;
        model.put("apiProvider", userRoles.stream().anyMatch(s -> s.endsWith(check)));
        return new ModelAndView("index", model);
    }

    /**
     * Retrieve the with the user data: currently on the username is added.
     * 
     * @return
     */
    @RequestMapping("/account")
    public ModelAndView account() {
        Map<String, Object> model = new HashMap<String, Object>();

        String username = userManager.getUserFullName();
        model.put("username", username);
        return new ModelAndView("account", model);
    }

    /**
     * Read the
     * 
     * @return {@link Response} entity containing the list of client app
     *         {@link ClientAppBasic} descriptors
     */
    @RequestMapping("/dev/apps")
    public @ResponseBody Response getAppList() {
        Response response = new Response();
        // read all the apps associated to the signed user
        List<ClientAppBasic> list = clientDetailsAdapter.getByDeveloperId(userManager.getUserId());
        response.setData(list);
        return response;
    }

    /**
     * Read the
     * 
     * @return {@link Response} entity containing the list of client app
     *         {@link ClientAppBasic} descriptors
     */
    @RequestMapping("/dev/apps/{clientId}")
    public @ResponseBody Response getApp(@PathVariable String clientId) {
        Response response = new Response();
        // read the app associated to the client
        ClientAppBasic app = clientDetailsAdapter.getByClientId(clientId);
        if (!app.getUserName().equals(userManager.getUserId().toString())) {
            throw new AccessDeniedException("Unauthorized");
        } else {
            response.setData(app);
        }

        return response;
    }
    
    @RequestMapping("/dev/apps/{clientId}/yaml")
    public void saveAppYaml(@PathVariable String clientId, HttpServletResponse response ) throws IOException  {
        // read the app associated to the client
        ClientAppBasic app = clientDetailsAdapter.getByClientId(clientId);
        if (!app.getUserName().equals(userManager.getUserId().toString())) {
            throw new AccessDeniedException("Unauthorized");
        } else {
           BootstrapClient bc = BootstrapClient.fromClientApp(app);
           
           //TODO fix in clientDetailsApp
           //we need to resolve username since it contains the id
           User developer = userManager.getOne(Long.parseLong(app.getUserName()));
           bc.setDeveloper(developer.getUsername());
           
           
           Yaml yaml = YamlUtils.getInstance(true, BootstrapClient.class);
           String res = yaml.dump(bc);
           
           //write as file
           response.setContentType("text/yaml");
           response.setHeader("Content-Disposition","attachment;filename="+bc.getName()+".yaml");
           ServletOutputStream out = response.getOutputStream();
           out.print(res);
           out.flush();
           out.close();
        }

    }


    /**
     * create a new client app given a container with the name only
     * 
     * @param appData
     * @return {@link Response} entity containing the stored app
     *         {@link ClientAppBasic} descriptor
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.POST, value = "/dev/apps")
    public @ResponseBody Response saveEmpty(@RequestBody ClientAppBasic appData) throws Exception {
        Response response = new Response();
        response.setData(clientDetailsAdapter.create(appData, userManager.getUserId()));
        return response;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/dev/apps/{clientId}")
    public @ResponseBody Response resetClientData(@PathVariable String clientId, @RequestParam String reset) {
        Response response = Response.error("unsupported reset for param " + String.valueOf(reset));

        switch (reset) {
        case "clientSecret":
            response = resetSecret(clientId, false);
            break;
        case "clientSecretMobile":
            response = resetSecret(clientId, true);
            break;

        }
        return response;
    }

    /**
     * Reset clientId or client secret
     * 
     * @param clientId
     * @param resetClientSecretMobile true to reset clientSecretMobile, false to
     *                                reset clientSecret
     * @return {@link Response} entity containing the stored app
     *         {@link ClientAppBasic} descriptor
     */
    protected Response resetSecret(String clientId, boolean resetClientSecretMobile) {
        Response response = new Response();
        userManager.checkClientIdOwnership(clientId);
        if (resetClientSecretMobile) {
            response.setData(clientDetailsAdapter.resetClientSecretMobile(clientId));
        } else {
            response.setData(clientDetailsAdapter.resetClientSecret(clientId));
        }
        return response;
    }

    /**
     * Delete the specified app
     * 
     * @param clientId
     * @return {@link Response} entity containing the deleted app
     *         {@link ClientAppBasic} descriptor
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/dev/apps/{clientId}")
    public @ResponseBody Response delete(@PathVariable String clientId) {
        Response response = new Response();
        userManager.checkClientIdOwnership(clientId);
        response.setData(clientDetailsAdapter.delete(clientId));
        return response;
    }

    /**
     * Update the client app
     * 
     * @param data
     * @param clientId
     * @return {@link Response} entity containing the updated app
     *         {@link ClientAppBasic} descriptor
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/dev/apps/{clientId}")
    public @ResponseBody Response update(@RequestBody ClientAppBasic data, @PathVariable String clientId) {
        Response response = new Response();
        userManager.checkClientIdOwnership(clientId);
        response.setData(clientDetailsAdapter.update(clientId, data));
        return response;
    }

    /**
     * Read the
     * 
     * @return {@link Response} entity containing the list of client app
     *         {@link ClientAppBasic} descriptors
     */
    @RequestMapping(value = "/dev/apps/{clientId}/claimmapping/validate", method = RequestMethod.POST)
    public @ResponseBody Response validateClaimMapping(@RequestBody ClientAppBasic data,
            @PathVariable String clientId) {
        Response response = new Response();
        User user = userManager.getUser();
        userManager.checkClientIdOwnership(clientId);
        ClientDetailsEntity client = clientDetailsRepo.findByClientId(clientId);
        ClientAppInfo appInfo = ClientAppInfo.convert(client.getAdditionalInformation());
        appInfo.setClaimMapping(data.getClaimMapping());
        try {
            Map<String, Object> claims = claimManager.validateUserClaimsForClientApp(user, appInfo, client.getScope());
            response.setData(claims);
        } catch (InvalidDefinitionException e) {
            response.setErrorMessage(e.getMessage());
            response.setResponseCode(RESPONSE.ERROR);
        }

        return response;
    }

    /**
     * Delete a specified API key
     * 
     * @param apiKey
     * @return
     */
    @DeleteMapping(value = "/dev/apikey/{clientId}/{apiKey:.*}")
    public @ResponseBody Response deleteKey(@PathVariable String clientId, @PathVariable String apiKey) {
        APIKey key = keyManager.findKey(apiKey);
        if (key != null) {
            userManager.checkClientIdOwnership(clientId);
            keyManager.deleteKey(apiKey);
        }
        return Response.ok(null);
    }

    /**
     * Update a specified API key
     * 
     * @param apiKey
     * @return
     */
    @PutMapping(value = "/dev/apikey/{clientId}/{apiKey:.*}")
    public @ResponseBody Response updateKey(@RequestBody APIKey body, @PathVariable String clientId,
            @PathVariable String apiKey) {
        APIKey key = keyManager.findKey(apiKey);
        if (key != null) {
            userManager.checkClientIdOwnership(clientId);
            //TODO check if current user is owner of key!!
            if (body.getValidity() != null && body.getValidity() > 0) {
                key = keyManager.updateKeyValidity(apiKey, body.getValidity());
            }
            
            if (body.getAdditionalInformation() != null) {
                key = keyManager.updateKeyData(apiKey, body.getAdditionalInformation());
            }
            
            if (body.getScope() != null) {
                key = keyManager.updateKeyScopes(apiKey, new HashSet<>(Arrays.asList(body.getScope())));
            }
            
            return Response.ok(key);
        }
        return Response.error("Key not found");
    }

    /**
	 * Create an API key with the specified properties (validity and additional info)
	 * @param apiKey
	 * @return created entity
	 */
	@PostMapping(value = "/dev/apikey/{clientId}")
	public @ResponseBody Response createKey(@RequestBody APIKey body, @PathVariable String clientId) {
		int validity = 0;
		if (body.getValidity() != null) {
		    validity = body.getValidity().intValue();
		}
        Set<String> scopes = new HashSet<>(Arrays.asList(body.getScope()));
	    APIKey keyObj = keyManager.createKey(clientId, null, validity , body.getAdditionalInformation(), scopes);
		return Response.ok(keyObj);
	}

    @GetMapping(value = "/dev/apikey/{clientId}")
    public @ResponseBody Response getClientKeys(@PathVariable String clientId) {
        return Response.ok(keyManager.getClientKeys(clientId));
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public Response processAccessError(AccessDeniedException ex) {
        return Response.error(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Response processValidationError(MethodArgumentNotValidException ex) {
        BindingResult br = ex.getBindingResult();
        List<FieldError> fieldErrors = br.getFieldErrors();
        StringBuilder builder = new StringBuilder();

        fieldErrors.forEach(fe -> builder.append(fe.getDefaultMessage()).append("\n"));

        return Response.error(builder.toString());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Response processIllegalError(IllegalArgumentException ex) {       
        logger.error(ex.getMessage());
        return Response.error(ex.getMessage());
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public Response processGenericError(Exception ex) {
        logger.error(ex.getMessage(), ex);
        return Response.error(ex.getMessage());
    }
}
