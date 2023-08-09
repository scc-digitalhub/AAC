/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.oauth.endpoint;

import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.accounts.model.UserAccount;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import it.smartcommunitylab.aac.model.ScopeType;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.model.ResponseMode;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import it.smartcommunitylab.aac.oauth.store.AuthorizationRequestStore;
import it.smartcommunitylab.aac.scope.Scope;
import it.smartcommunitylab.aac.scope.ScopeRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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

@Hidden
@Controller
//@SessionAttributes("authorizationRequest")
public class UserApprovalEndpoint implements InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String ACCESS_CONFIRMATION_URL = "/oauth/confirm_access";
    public static final String APPROVAL_URL = "/oauth/approval";

    private static final String errorPage = "forward:" + ErrorEndpoint.ERROR_URL;
    private static final String responsePage = "forward:" + AuthorizationEndpoint.AUTHORIZED_URL;
    private static final String formPage = "forward:" + AuthorizationEndpoint.FORM_POST_URL;

    //    private static final String SCOPE_PREFIX = OAuth2Utils.SCOPE_PREFIX;

    @Autowired
    private AuthorizationRequestStore oauth2AuthorizationRequestRepository;

    @Autowired
    private UserApprovalHandler userApprovalHandler;

    @Autowired
    private OAuth2ClientDetailsService oauth2ClientDetailsService;

    @Autowired
    private ScopeRegistry scopeRegistry;

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(userApprovalHandler, "user approval handler is required");
        Assert.notNull(oauth2ClientDetailsService, "oauth2 client service is required");
        Assert.notNull(oauth2AuthorizationRequestRepository, "request repository is required");
    }

    @RequestMapping(value = ACCESS_CONFIRMATION_URL, method = RequestMethod.GET)
    public ModelAndView accessConfirmation(
        @RequestParam String key,
        Map<String, Object> model,
        Authentication authentication,
        HttpServletRequest request,
        Locale locale
    ) {
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
            Map<String, Object> approvalParameters = userApprovalHandler.getUserApprovalRequest(
                authorizationRequest,
                authentication
            );
            model.putAll(approvalParameters);

            // load realm props
            String realm = clientDetails.getRealm();
            model.put("realm", realm);
            model.put("displayName", realm);

            // add client info
            model.put("client", clientDetails);

            // add user info
            String userName = StringUtils.hasText(userDetails.getUsername())
                ? userDetails.getUsername()
                : userDetails.getSubjectId();
            //            String fullName = userDetails.getFullName();
            model.put("fullname", userName);

            // add account info
            UserAccount account = userDetails.getIdentities().stream().findFirst().orElseThrow().getAccount();
            model.put("account", account);

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

            // filter already approved scopes
            List<Scope> approvalResources = resources
                .stream()
                .filter(s -> !approvedScopes.contains(s.getScope()))
                .collect(Collectors.toList());
            model.put("resources", approvalResources);

            List<Scope> hiddenResources = resources
                .stream()
                .filter(s -> approvedScopes.contains(s.getScope()))
                .collect(Collectors.toList());
            model.put("hiddenResources", hiddenResources);

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

            return new ModelAndView("user-approval", model);
        } catch (RuntimeException e) {
            // send to error page
            model.put("error", e.getMessage());

            return new ModelAndView(errorPage, model);
        }
    }

    @RequestMapping(value = APPROVAL_URL, method = RequestMethod.POST)
    public ModelAndView approve(
        @RequestParam Map<String, String> approvalParameters,
        Map<String, Object> model,
        Authentication authentication
    ) {
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
            // check if response is requested via post
            String responseMode = (String) authorizationRequest.getExtensions().get("response_mode");
            if (ResponseMode.FORM_POST.getValue().equals(responseMode)) {
                // process as authorized via form post
                return new ModelAndView(formPage);
            }

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
