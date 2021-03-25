package it.smartcommunitylab.aac.oauth.endpoint;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import springfox.documentation.annotations.ApiIgnore;

@ApiIgnore
@Controller
@SessionAttributes("authorizationRequest")
public class UserApprovalEndpoint {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String SCOPE_PREFIX = OAuth2Utils.SCOPE_PREFIX;

    @Autowired
    private OAuth2ClientDetailsService clientDetailsService;

    @RequestMapping("/oauth/confirm_access")
    public ModelAndView getAccessConfirmation(Map<String, Object> model, HttpServletRequest request) throws Exception {

        AuthorizationRequest authorizationRequest = (AuthorizationRequest) model.get("authorizationRequest");
        String clientId = authorizationRequest.getClientId();

        OAuth2ClientDetails clientDetails;
        try {
            clientDetails = clientDetailsService.loadClientByClientId(clientId);
        } catch (ClientRegistrationException e) {
            // non existing client, drop
            throw new InvalidRequestException("invalid client");
        }

        // we have a list of scopes in model or request
        @SuppressWarnings("unchecked")
        Map<String, String> scopes = (Map<String, String>) (model.containsKey("scopes") ? model.get("scopes")
                : request.getAttribute("scopes"));

        if (scopes == null || scopes.isEmpty()) {
            // nothing to show?
        }

        // TODO resolve scopes to user resources
        List<Map.Entry<String, String>> resources = new ArrayList<>();
        for (String scope : scopes.keySet()) {
            AbstractMap.SimpleEntry<String, String> entry = new AbstractMap.SimpleEntry<>(scope, scope);
            resources.add(entry);
        }

        model.put("resources", resources);

        // re-add to model
        model.put("scopes", scopes);

        // add client info
        String clientName = StringUtils.hasText(clientDetails.getName()) ? clientDetails.getName() : clientId;
        model.put("clientName", clientName);

        // TODO add spaces
//        model.put("spaces", spaces == null ? Collections.emptyMap() : spaces);
        model.put("spaces", Collections.emptyMap());

        // TODO handle csrf
        if (request.getAttribute("_csrf") != null) {
            model.put("_csrf", request.getAttribute("_csrf"));
        }

        logger.trace("call view with model " + model);
        return new ModelAndView("access_confirmation", model);
    }
}
