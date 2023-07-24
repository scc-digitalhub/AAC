package it.smartcommunitylab.aac.oauth.token;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.service.ClientDetailsService;
import it.smartcommunitylab.aac.model.ScopeType;
import it.smartcommunitylab.aac.oauth.AACOAuth2AccessToken;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import it.smartcommunitylab.aac.scope.Scope;
import it.smartcommunitylab.aac.scope.ScopeApprover;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.security.oauth2.common.exceptions.InvalidScopeException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;

public class ClientCredentialsTokenGranter extends AbstractTokenGranter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String GRANT_TYPE = "client_credentials";
    private boolean allowRefresh = false;

    private ClientDetailsService clientService;

    public ClientCredentialsTokenGranter(
        AuthorizationServerTokenServices tokenServices,
        OAuth2ClientDetailsService clientDetailsService,
        OAuth2RequestFactory requestFactory
    ) {
        this(tokenServices, clientDetailsService, requestFactory, GRANT_TYPE);
    }

    protected ClientCredentialsTokenGranter(
        AuthorizationServerTokenServices tokenServices,
        OAuth2ClientDetailsService clientDetailsService,
        OAuth2RequestFactory requestFactory,
        String grantType
    ) {
        super(tokenServices, clientDetailsService, requestFactory, grantType);
    }

    public void setAllowRefresh(boolean allowRefresh) {
        this.allowRefresh = allowRefresh;
    }

    @Override
    public OAuth2AccessToken grant(String grantType, TokenRequest tokenRequest) {
        OAuth2AccessToken token = super.grant(grantType, tokenRequest);

        if (token != null) {
            logger.trace(
                "grant access token for client " +
                tokenRequest.getClientId() +
                " request " +
                tokenRequest.getRequestParameters().toString()
            );

            if (!allowRefresh) {
                AACOAuth2AccessToken norefresh = new AACOAuth2AccessToken(token);
                // The spec says that client credentials should not be allowed to get a refresh
                // token
                norefresh.setRefreshToken(null);
                token = norefresh;
                // TODO we should also remove the refresh token from DB
            }
        }
        return token;
    }

    @Override
    protected void validateScope(Set<String> scopes, OAuth2Authentication authentication) {
        if (scopeRegistry != null && clientService != null) {
            try {
                String clientId = authentication.getOAuth2Request().getClientId();
                it.smartcommunitylab.aac.core.ClientDetails clientDetails = clientService.loadClient(clientId);

                // check each scope is of type client and approved
                for (String s : scopes) {
                    try {
                        Scope scope = scopeRegistry.getScope(s);
                        if (ScopeType.CLIENT != scope.getType() && ScopeType.GENERIC != scope.getType()) {
                            throw new InvalidScopeException("Unauthorized scope: " + s);
                        }

                        ScopeApprover sa = scopeRegistry.getScopeApprover(s);
                        if (sa == null) {
                            // this scope is undecided so skip
                            continue;
                        }

                        Approval approval = sa.approveClientScope(s, clientDetails, scopes);
                        if (approval != null) {
                            if (!approval.isApproved()) {
                                throw new InvalidScopeException("Unauthorized scope: " + s);
                            }
                        }
                    } catch (NoSuchScopeException | SystemException | InvalidDefinitionException e) {
                        throw new InvalidScopeException("Unauthorized scope: " + s);
                    }
                }
            } catch (NoSuchClientException e1) {
                throw new InvalidClientException("Invalid client");
            }
        }
    }

    public void setClientService(ClientDetailsService clientService) {
        this.clientService = clientService;
    }
}
