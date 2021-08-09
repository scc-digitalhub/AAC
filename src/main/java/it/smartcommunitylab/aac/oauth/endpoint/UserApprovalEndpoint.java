package it.smartcommunitylab.aac.oauth.endpoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.approval.UserApprovalHandler;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.core.RealmManager;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import it.smartcommunitylab.aac.dto.CustomizationBean;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.model.ScopeType;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import it.smartcommunitylab.aac.oauth.store.AuthorizationRequestStore;
import it.smartcommunitylab.aac.scope.Scope;
import it.smartcommunitylab.aac.scope.ScopeRegistry;
import springfox.documentation.annotations.ApiIgnore;

@ApiIgnore
@Controller
//@SessionAttributes("authorizationRequest")
public class UserApprovalEndpoint implements InitializingBean {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String ACCESS_CONFIRMATION_URL = "/oauth/confirm_access";
    public static final String APPROVAL_URL = "/oauth/approval";

    private static final String errorPage = "forward:" + ErrorEndpoint.ERROR_URL;
    private static final String responsePage = "forward:" + AuthorizationEndpoint.AUTHORIZED_URL;

//    private static final String SCOPE_PREFIX = OAuth2Utils.SCOPE_PREFIX;

    @Autowired
    private AuthorizationRequestStore oauth2AuthorizationRequestRepository;

    @Autowired
    private UserApprovalHandler userApprovalHandler;

    @Autowired
    private OAuth2ClientDetailsService oauth2ClientDetailsService;

    @Autowired
    private RealmManager realmManager;

    @Autowired
    private ScopeRegistry scopeRegistry;

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(userApprovalHandler, "user approval handler is required");
        Assert.notNull(oauth2ClientDetailsService, "oauth2 client service is required");
        Assert.notNull(oauth2AuthorizationRequestRepository, "request repository is required");
    }

    @RequestMapping(value = ACCESS_CONFIRMATION_URL, method = RequestMethod.GET)
    public ModelAndView accessConfirmation(@RequestParam String key,
            Map<String, Object> model,
            Authentication authentication, HttpServletRequest request) {

        if (!(authentication instanceof UserAuthentication) || !authentication.isAuthenticated()) {
            throw new InsufficientAuthenticationException("Invalid user authentication");
        }

        try {
            if (!StringUtils.hasText(key)) {
                throw new IllegalArgumentException("Missing or invalid request key");
            }

            AuthorizationRequest authorizationRequest = oauth2AuthorizationRequestRepository.find(key);
            if (authorizationRequest == null) {
                throw new IllegalArgumentException("Missing or invalid request");
            }

            model.put("key", key);

            UserAuthentication userAuth = (UserAuthentication) authentication;
            UserDetails userDetails = userAuth.getUser();

            String clientId = authorizationRequest.getClientId();
            OAuth2ClientDetails clientDetails = oauth2ClientDetailsService.loadClientByClientId(clientId);

            // we ask approvalHandler to build parameters
            Map<String, Object> approvalParameters = userApprovalHandler.getUserApprovalRequest(authorizationRequest,
                    authentication);
            model.putAll(approvalParameters);

            // load realm customizations
            String realm = clientDetails.getRealm();

            try {
                String displayName = null;
                Realm re = null;
                Map<String, String> resources = new HashMap<>();
                if (!realm.equals(SystemKeys.REALM_COMMON)) {
                    re = realmManager.getRealm(realm);
                    displayName = re.getName();
                    CustomizationBean gcb = re.getCustomization("global");
                    if (gcb != null) {
                        resources.putAll(gcb.getResources());
                    }
                    CustomizationBean lcb = re.getCustomization("approval");
                    if (lcb != null) {
                        resources.putAll(lcb.getResources());
                    }
                }

                model.put("displayName", displayName);
                model.put("customization", resources);
            } catch (NoSuchRealmException e) {
                throw new IllegalArgumentException("Invalid realm");
            }

            // add client info
            String clientName = StringUtils.hasText(clientDetails.getName()) ? clientDetails.getName() : clientId;
            model.put("clientName", clientName);

            // add user info
            String userName = StringUtils.hasText(userDetails.getUsername()) ? userDetails.getUsername()
                    : userDetails.getSubjectId();
            model.put("username", userName);

            // we have a list of scopes in model
            Set<String> scopes = delimitedStringToSet((String) model.get("scope"));

            // we should also have approved and approval scopes
            Set<String> approvedScopes = delimitedStringToSet((String) model.get("approved.scope"));
            Set<String> approvalScopes = delimitedStringToSet((String) model.get("approval.scope"));

            if (scopes == null || scopes.isEmpty()) {
                // nothing to show?
            }

            // resolve scopes to user resources via registry
            List<Scope> resources = new ArrayList<>();
            for (String scope : scopes) {
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

            // add spaces
            Set<String> spaces = delimitedStringToSet((String) model.get("spaces"));
            model.put("spaces", spaces);

            // TODO handle csrf
            if (request.getAttribute("_csrf") != null) {
                model.put("_csrf", request.getAttribute("_csrf"));
            }

            // add form action
            // TODO handle via urlBuilder
            model.put("formAction", APPROVAL_URL);

            logger.trace("call view with model " + model);
            return new ModelAndView("access_confirmation", model);

        } catch (RuntimeException e) {
            // send to error page
            model.put("error", e.getMessage());

            return new ModelAndView(errorPage, model);
        }

    }

    @RequestMapping(value = APPROVAL_URL, method = RequestMethod.POST)
    public ModelAndView approve(@RequestParam Map<String, String> approvalParameters,
            Map<String, Object> model,
            Authentication authentication) {
        if (!(authentication instanceof UserAuthentication) || !authentication.isAuthenticated()) {
            throw new InsufficientAuthenticationException("Invalid user authentication");
        }

        try {
            String key = approvalParameters.get("key");
            if (!StringUtils.hasText(key)) {
                throw new IllegalArgumentException("Missing or invalid request key");
            }

            AuthorizationRequest authorizationRequest = oauth2AuthorizationRequestRepository.find(key);
            if (authorizationRequest == null) {
                throw new IllegalArgumentException("Missing or invalid request");
            }

            // update approval parameters and status
            authorizationRequest.setApprovalParameters(approvalParameters);
            authorizationRequest = userApprovalHandler.updateAfterApproval(authorizationRequest, authentication);

            boolean approved = userApprovalHandler.isApproved(authorizationRequest, authentication);
            authorizationRequest.setApproved(approved);

            // update in repo
            oauth2AuthorizationRequestRepository.store(authorizationRequest, key);

            // forward to authorization endpoint for response
            // no need to append key param since we forward all params already
            return new ModelAndView(responsePage);

        } catch (RuntimeException e) {
            // send to error page
            model.put("error", e.getMessage());

            return new ModelAndView(errorPage, model);
        }

    }

//    @RequestMapping(value = ACCESS_CONFIRMATION_URL, method = RequestMethod.GET)
//    public ModelAndView getAccessConfirmation(Map<String, Object> model, HttpServletRequest request) throws Exception {
//
//        AuthorizationRequest authorizationRequest = (AuthorizationRequest) model.get("authorizationRequest");
//        String clientId = authorizationRequest.getClientId();
//        Map<String, String> approvalParameters = authorizationRequest.getApprovalParameters();
//
//        OAuth2ClientDetails clientDetails;
//        try {
//            clientDetails = clientDetailsService.loadClientByClientId(clientId);
//        } catch (ClientRegistrationException e) {
//            // non existing client, drop
//            throw new InvalidRequestException("invalid client");
//        }
//
//        UserAuthentication userAuth = authHelper.getUserAuthentication();
//        if (userAuth == null) {
//            throw new InvalidRequestException("invalid user");
//        }
//
//        UserDetails userDetails = userAuth.getUser();
//        String realm = clientDetails.getRealm();
//
//        Realm re = null;
//        CustomizationBean cb = null;
//        if (!realm.equals(SystemKeys.REALM_COMMON)) {
//            re = realmManager.getRealm(realm);
//            cb = re.getCustomization("approval");
//        }
//
//        if (cb != null) {
//            model.put("customization", cb.getResources());
//        } else {
//            model.put("customization", null);
//        }
//
//        // add client info
//        String clientName = StringUtils.hasText(clientDetails.getName()) ? clientDetails.getName() : clientId;
//        model.put("clientName", clientName);
//
//        // add user info
//        String userName = StringUtils.hasText(userDetails.getUsername()) ? userDetails.getUsername()
//                : userDetails.getSubjectId();
//        model.put("userName", userName);
//
//        // we have a list of scopes in model or request
//        @SuppressWarnings("unchecked")
//        Map<String, String> scopes = (Map<String, String>) (model.containsKey("scopes") ? model.get("scopes")
//                : request.getAttribute("scopes"));
//
//        if (scopes == null || scopes.isEmpty()) {
//            // nothing to show?
//        }
//
//        // resolve scopes to user resources via registry
//        List<Scope> resources = new ArrayList<>();
//        for (String scope : scopes.keySet()) {
//            Scope s = scopeRegistry.findScope(scope);
//            if (s == null) {
//                // build empty model
//                s = new Scope(scope);
//                s.setName("scope.name." + scope);
//                s.setDescription("scope.description." + scope);
//                s.setType(ScopeType.GENERIC);
//            }
//
//            resources.add(s);
//        }
//
//        model.put("resources", resources);
//
//        // re-add to model
//        model.put("scopes", scopes);
//
//        // add spaces
//        @SuppressWarnings("unchecked")
//        Collection<String> spaces = (Collection<String>) (model.containsKey("spaces") ? model.get("spaces")
//                : request.getAttribute("spaces"));
//        model.put("spaces", spaces);
//
//        // TODO handle csrf
//        if (request.getAttribute("_csrf") != null) {
//            model.put("_csrf", request.getAttribute("_csrf"));
//        }
//
//        // add form action
//        // TODO handle per realm
//        model.put("formAction", "/oauth/authorize");
//
//        logger.trace("call view with model " + model);
//        return new ModelAndView("access_confirmation", model);
//    }

    private Set<String> delimitedStringToSet(String str) {
        String[] tokens = StringUtils.delimitedListToStringArray(str, " ");
        return new LinkedHashSet<>(Arrays.asList(tokens));
    }
}
