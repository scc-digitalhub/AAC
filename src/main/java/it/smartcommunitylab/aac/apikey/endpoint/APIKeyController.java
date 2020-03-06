package it.smartcommunitylab.aac.apikey.endpoint;

import java.util.Map;

import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import it.smartcommunitylab.aac.apikey.manager.APIKeyManager;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.dto.APIKey;
import it.smartcommunitylab.aac.model.Response;

/**
 * @author raman
 *
 */
@Controller
@Api(tags = { "AAC ApiKey" })
public class APIKeyController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private APIKeyManager keyManager;

    @ApiOperation(value = "Validate key")
    @GetMapping(value = "/apikeycheck/{apiKey:.*}")
    public @ResponseBody Map<String, Object> findKey(@PathVariable String apiKey,
            Authentication auth) throws EntityNotFoundException {

        // delegate
        return findKeyByParam(apiKey, auth);
    }

    @ApiOperation(value = "Validate key as parameter")
    @GetMapping(value = "/apikeycheck")
    public @ResponseBody Map<String, Object> findKeyByParam(@RequestParam String apiKey,
            Authentication auth) throws EntityNotFoundException {
//        String clientId = SecurityUtils.getOAuthOrBasicClientId(auth, true);

        if (keyManager.isKeyValid(apiKey)) {
            // TODO evaluate restricting access to owner or filter response
            // fetch a fully populated key
            APIKey apikey = keyManager.getKey(apiKey);

            // manually build a result
            Map<String, Object> json = APIKey.toMap(apikey);

            return json;
        }

        // TODO evaluate returning a proper response instead of an error
        throw new EntityNotFoundException();
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
