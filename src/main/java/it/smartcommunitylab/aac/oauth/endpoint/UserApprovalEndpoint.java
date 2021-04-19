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

import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.auth.UserAuthenticationToken;
import it.smartcommunitylab.aac.model.ScopeType;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import it.smartcommunitylab.aac.scope.Scope;
import it.smartcommunitylab.aac.scope.ScopeRegistry;
import springfox.documentation.annotations.ApiIgnore;

@ApiIgnore
@Controller
@SessionAttributes("authorizationRequest")
public class UserApprovalEndpoint {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String SCOPE_PREFIX = OAuth2Utils.SCOPE_PREFIX;

    @Autowired
    private OAuth2ClientDetailsService clientDetailsService;

    @Autowired
    private ScopeRegistry scopeRegistry;

    @Autowired
    private AuthenticationHelper authHelper;

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

        UserAuthenticationToken userAuth = authHelper.getUserAuthentication();
        if (userAuth == null) {
            throw new InvalidRequestException("invalid user");
        }

        UserDetails userDetails = userAuth.getUser();

        // add client info
        String clientName = StringUtils.hasText(clientDetails.getName()) ? clientDetails.getName() : clientId;
        model.put("clientName", clientName);

        // add user info
        String userName = StringUtils.hasText(userDetails.getUsername()) ? userDetails.getUsername()
                : userDetails.getSubjectId();
        model.put("userName", userName);

        // we have a list of scopes in model or request
        @SuppressWarnings("unchecked")
        Map<String, String> scopes = (Map<String, String>) (model.containsKey("scopes") ? model.get("scopes")
                : request.getAttribute("scopes"));

        if (scopes == null || scopes.isEmpty()) {
            // nothing to show?
        }

        // resolve scopes to user resources via registry
        List<Scope> resources = new ArrayList<>();
        for (String scope : scopes.keySet()) {
            Scope s = scopeRegistry.findScope(scope);
            if (s == null) {
                // build empty model
                s = new Scope(scope);
                s.setName("scope.name." + scope);
                s.setDescription("scope.description." + scope);
                s.setType(ScopeType.GENERIC);
            }

            resources.add(s);
        }

        model.put("resources", resources);

        // re-add to model
        model.put("scopes", scopes);

        // TODO add spaces
//        model.put("spaces", spaces == null ? Collections.emptyMap() : spaces);
        model.put("spaces", Collections.emptyMap());

        // TODO handle csrf
        if (request.getAttribute("_csrf") != null) {
            model.put("_csrf", request.getAttribute("_csrf"));
        }

        // add form action
        // TODO handle per realm
        model.put("formAction", "/oauth/authorize");

        logger.trace("call view with model " + model);
        return new ModelAndView("access_confirmation", model);
    }
}
