package it.smartcommunitylab.aac.oauth.token;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.TokenGranter;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.auth.UserAuthenticationToken;
import it.smartcommunitylab.aac.core.service.UserTranslatorService;
import it.smartcommunitylab.aac.model.ScopeType;
import it.smartcommunitylab.aac.scope.Scope;
import it.smartcommunitylab.aac.scope.ScopeApprover;
import it.smartcommunitylab.aac.scope.ScopeRegistry;

public abstract class AbstractTokenGranter implements TokenGranter {

    protected final Log logger = LogFactory.getLog(getClass());

    private final AuthorizationServerTokenServices tokenServices;

    private final ClientDetailsService clientDetailsService;

    private final OAuth2RequestFactory requestFactory;

    private final String grantType;

    private it.smartcommunitylab.aac.core.service.ClientDetailsService clientService;
    private ScopeRegistry scopeRegistry;
    private UserTranslatorService userTranslatorService;

    protected AbstractTokenGranter(AuthorizationServerTokenServices tokenServices,
            ClientDetailsService clientDetailsService, OAuth2RequestFactory requestFactory, String grantType) {
        this.clientDetailsService = clientDetailsService;
        this.grantType = grantType;
        this.tokenServices = tokenServices;
        this.requestFactory = requestFactory;
    }

    public OAuth2AccessToken grant(String grantType, TokenRequest tokenRequest) {

        if (!this.grantType.equals(grantType)) {
            return null;
        }

        String clientId = tokenRequest.getClientId();
        ClientDetails client = clientDetailsService.loadClientByClientId(clientId);
        validateGrantType(grantType, client);
        tokenRequest.setScope(
                validateScopes(getOAuth2Authentication(client, tokenRequest), client, tokenRequest.getScope()));

        logger.debug("Getting access token for: " + clientId);

        return getAccessToken(client, tokenRequest);

    }

    protected OAuth2AccessToken getAccessToken(ClientDetails client, TokenRequest tokenRequest) {
        return tokenServices.createAccessToken(getOAuth2Authentication(client, tokenRequest));
    }

    protected OAuth2Authentication getOAuth2Authentication(ClientDetails client, TokenRequest tokenRequest) {
        OAuth2Request storedOAuth2Request = requestFactory.createOAuth2Request(client, tokenRequest);
        return new OAuth2Authentication(storedOAuth2Request, null);
    }

    protected void validateGrantType(String grantType, ClientDetails clientDetails) {
        Collection<String> authorizedGrantTypes = clientDetails.getAuthorizedGrantTypes();
        if (authorizedGrantTypes == null || authorizedGrantTypes.isEmpty()
                || !authorizedGrantTypes.contains(grantType)) {
            throw new InvalidClientException("Unauthorized grant type: " + grantType);
        }
    }

    protected Set<String> validateScopes(OAuth2Authentication authentication, ClientDetails client,
            Set<String> scopes) {

        if (clientService == null || scopeRegistry == null || userTranslatorService == null) {
            return scopes;
        } else {
            try {
                // fetch client
                it.smartcommunitylab.aac.core.ClientDetails clientDetails = clientService
                        .loadClient(client.getClientId());
                UserDetails userDetails = null;

                // check if userAuth is present
                Authentication userAuth = authentication.getUserAuthentication();
                if (userAuth != null && userAuth instanceof UserAuthenticationToken) {
                    userDetails = ((UserAuthenticationToken) userAuth).getUser();
                }

                Set<String> approvedScopes = new HashSet<>();

                for (String s : scopes) {
                    try {
                        Scope scope = scopeRegistry.getScope(s);
                        ScopeApprover sa = scopeRegistry.getScopeApprover(s);
                        if (sa == null) {
                            // this scope is undecided so skip
                            continue;
                        }

                        Approval approval = null;
                        if (ScopeType.CLIENT == scope.getType()) {
                            approval = sa.approveClientScope(s, clientDetails, scopes);
                        }
                        if (ScopeType.USER == scope.getType() && userDetails != null) {
                            approval = sa.approveUserScope(s,
                                    userTranslatorService.translate(userDetails, sa.getRealm()), clientDetails,
                                    scopes);
                        }
                        if (ScopeType.GENERIC == scope.getType()) {
                            if (userDetails != null) {
                                approval = sa.approveUserScope(s,
                                        userTranslatorService.translate(userDetails, sa.getRealm()),
                                        clientDetails, scopes);
                            } else {
                                approval = sa.approveClientScope(s, clientDetails, scopes);
                            }
                        }

                        if (approval != null) {
                            if (!approval.isApproved()) {
                                // deny the request
                                throw new InvalidClientException("Unauthorized client for scope: " + s);
                            } else {
                                approvedScopes.add(s);
                            }
                        }

                    } catch (NoSuchScopeException | SystemException | InvalidDefinitionException e) {
                        // ignore
                    }

                }

                return approvedScopes;

            } catch (NoSuchClientException e) {
                throw new InvalidClientException("Invalid or unauthorized client");
            }
        }

    }

    protected AuthorizationServerTokenServices getTokenServices() {
        return tokenServices;
    }

    protected OAuth2RequestFactory getRequestFactory() {
        return requestFactory;
    }

    public void setClientService(it.smartcommunitylab.aac.core.service.ClientDetailsService clientService) {
        this.clientService = clientService;
    }

    public void setScopeRegistry(ScopeRegistry scopeRegistry) {
        this.scopeRegistry = scopeRegistry;
    }

    public void setUserTranslatorService(UserTranslatorService userTranslatorService) {
        this.userTranslatorService = userTranslatorService;
    }

}
