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

package it.smartcommunitylab.aac.oauth.approval;

import it.smartcommunitylab.aac.clients.service.ClientDetailsService;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.model.SpaceRole;
import it.smartcommunitylab.aac.roles.claims.SpacesClaimsExtractor;
import it.smartcommunitylab.aac.users.auth.UserAuthentication;
import it.smartcommunitylab.aac.users.model.User;
import it.smartcommunitylab.aac.users.model.UserDetails;
import it.smartcommunitylab.aac.users.service.UserService;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.approval.UserApprovalHandler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class SpacesApprovalHandler implements UserApprovalHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ClientDetailsService clientDetailsService;
    private final UserService userService;

    public SpacesApprovalHandler(ClientDetailsService clientService, UserService userService) {
        Assert.notNull(clientService, "client details service is required");
        Assert.notNull(userService, "user service is required");
        this.clientDetailsService = clientService;
        this.userService = userService;
    }

    @Override
    public boolean isApproved(AuthorizationRequest authorizationRequest, Authentication userAuthentication) {
        return authorizationRequest.isApproved();
    }

    @Override
    public AuthorizationRequest checkForPreApproval(
        AuthorizationRequest authorizationRequest,
        Authentication userAuth
    ) {
        if (userAuth == null || !(userAuth instanceof UserAuthentication)) {
            throw new InvalidRequestException("approval requires a valid user authentication");
        }

        UserDetails userDetails = ((UserAuthentication) userAuth).getUserDetails();

        // fetch details
        String clientId = authorizationRequest.getClientId();
        ClientDetails clientDetails;
        try {
            clientDetails = clientDetailsService.loadClient(clientId);
        } catch (NoSuchClientException | ClientRegistrationException e) {
            // non existing client, drop
            throw new InvalidRequestException("invalid client");
        }

        // see if the user has to perform the space selection
        String uniqueSpaces = clientDetails.getHookUniqueSpaces();
        if (StringUtils.hasText(uniqueSpaces)) {
            // fetch spaces list from context
            Set<String> spaces = getUniqueSpaces(userDetails, uniqueSpaces);
            if (!spaces.isEmpty()) {
                //                // reset params since these are supposed to be immutable
                //                Map<String, String> params = new HashMap<>();
                //                params.putAll(authorizationRequest.getApprovalParameters());
                //                params.put(SPACE_SELECTION_APPROVAL_REQUIRED, "true");
                ////                params.put(SPACE_SELECTION_APPROVAL_DONE, "false");
                //
                //                // TODO evaluate usage of extension instead of mangling with approvalParameters
                //                authorizationRequest.setApprovalParameters(params);

                // we also reset approved status to ensure approval endpoint is invoked
                authorizationRequest.setApproved(false);
            }
        }

        return authorizationRequest;
    }

    @Override
    public AuthorizationRequest updateAfterApproval(
        AuthorizationRequest authorizationRequest,
        Authentication userAuth
    ) {
        // we don't actually care about what client asked, we check if user has
        // performed selection, we could reject authorization if client asked for space
        // selection but that would break base flow and we treat this as an extension
        String spaceSelection = authorizationRequest.getApprovalParameters().get("space_selection");
        if (StringUtils.hasText(spaceSelection)) {
            // we want to validate the selection
            if (userAuth == null || !(userAuth instanceof UserAuthentication)) {
                throw new InvalidRequestException("approval requires a valid user authentication");
            }

            UserDetails userDetails = ((UserAuthentication) userAuth).getUserDetails();

            // fetch details
            String clientId = authorizationRequest.getClientId();
            ClientDetails clientDetails;
            try {
                clientDetails = clientDetailsService.loadClient(clientId);
            } catch (NoSuchClientException | ClientRegistrationException e) {
                // non existing client, drop
                throw new InvalidRequestException("invalid client");
            }

            // see if the user has to perform the space selection
            String uniqueSpaces = clientDetails.getHookUniqueSpaces();
            if (StringUtils.hasText(uniqueSpaces)) {
                // fetch spaces list from context
                Set<String> spaces = getUniqueSpaces(userDetails, uniqueSpaces);
                if (spaces.contains(spaceSelection)) {
                    // space is among those available for selection
                    // export selection as extension
                    Map<String, Serializable> extensions = new HashMap<>();
                    extensions.putAll(authorizationRequest.getExtensions());
                    extensions.put(SpacesClaimsExtractor.SPACES_EXTENSIONS_KEY, spaceSelection);
                    authorizationRequest.setExtensions(extensions);
                }
            }
        }

        return authorizationRequest;
    }

    @Override
    public Map<String, Object> getUserApprovalRequest(
        AuthorizationRequest authorizationRequest,
        Authentication userAuth
    ) {
        // build view model, need to return non null
        Map<String, Object> model = new HashMap<String, Object>();

        // space selection, we need to check again
        if (userAuth == null || !(userAuth instanceof UserAuthentication)) {
            throw new InvalidRequestException("approval requires a valid user authentication");
        }

        UserDetails userDetails = ((UserAuthentication) userAuth).getUserDetails();

        // fetch details
        String clientId = authorizationRequest.getClientId();
        ClientDetails clientDetails;
        try {
            clientDetails = clientDetailsService.loadClient(clientId);
        } catch (NoSuchClientException | ClientRegistrationException e) {
            // non existing client, drop
            throw new InvalidRequestException("invalid client");
        }

        // see if the user has to perform the space selection
        Set<String> spaces = Collections.emptySet();
        String uniqueSpaces = clientDetails.getHookUniqueSpaces();
        if (StringUtils.hasText(uniqueSpaces)) {
            // fetch spaces list from context
            spaces = getUniqueSpaces(userDetails, uniqueSpaces);
        }

        if (!spaces.isEmpty()) {
            model.put("spaces", StringUtils.collectionToDelimitedString(spaces, " "));
        }

        return model;
    }

    private Set<String> getUniqueSpaces(UserDetails userDetails, String uniqueSpaces) {
        User user = userService.getUser(userDetails);
        // Set<SpaceRole> roles = user.getSpaceRoles();
        Set<SpaceRole> roles = Collections.emptySet();

        // filter and flatmap everything under context
        Set<String> spaces = roles
            .stream()
            .filter(r -> (r.getContext() != null && r.getContext().startsWith(uniqueSpaces)))
            .map(r -> r.getSpace())
            .collect(Collectors.toSet());

        return spaces;
    }
}
