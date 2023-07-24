package it.smartcommunitylab.aac.oauth.token;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.auth.RealmWrappedAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import it.smartcommunitylab.aac.core.service.ClientDetailsService;
import it.smartcommunitylab.aac.core.service.UserService;
import it.smartcommunitylab.aac.model.ScopeType;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.oauth.AACOAuth2AccessToken;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import it.smartcommunitylab.aac.password.auth.UsernamePasswordAuthenticationToken;
import it.smartcommunitylab.aac.scope.Scope;
import it.smartcommunitylab.aac.scope.ScopeApprover;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.security.oauth2.common.exceptions.InvalidScopeException;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedUserException;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;

public class ResourceOwnerPasswordTokenGranter extends AbstractTokenGranter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String GRANT_TYPE = "password";
    private boolean allowRefresh = true;

    private final AuthenticationManager authenticationManager;

    private ClientDetailsService clientService;
    private UserService userService;

    public ResourceOwnerPasswordTokenGranter(
        AuthenticationManager authenticationManager,
        AuthorizationServerTokenServices tokenServices,
        OAuth2ClientDetailsService clientDetailsService,
        OAuth2RequestFactory requestFactory
    ) {
        this(authenticationManager, tokenServices, clientDetailsService, requestFactory, GRANT_TYPE);
    }

    protected ResourceOwnerPasswordTokenGranter(
        AuthenticationManager authenticationManager,
        AuthorizationServerTokenServices tokenServices,
        OAuth2ClientDetailsService clientDetailsService,
        OAuth2RequestFactory requestFactory,
        String grantType
    ) {
        super(tokenServices, clientDetailsService, requestFactory, grantType);
        this.authenticationManager = authenticationManager;
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
    protected OAuth2Authentication getOAuth2Authentication(ClientDetails client, TokenRequest tokenRequest) {
        Map<String, String> parameters = new LinkedHashMap<String, String>(tokenRequest.getRequestParameters());
        String username = parameters.get("username");
        String password = parameters.get("password");
        // Protect from downstream leaks of password
        parameters.remove("password");

        AbstractAuthenticationToken passwordAuth = new UsernamePasswordAuthenticationToken(username, password);
        passwordAuth.setDetails(parameters);

        Authentication userAuth = passwordAuth;
        // workaround to select internal provider for realm
        // TODO rewrite and extend to identity proper provider or provide a fallback via
        // authmanager
        if (client instanceof OAuth2ClientDetails) {
            String realm = ((OAuth2ClientDetails) client).getRealm();
            userAuth = new RealmWrappedAuthenticationToken(passwordAuth, realm, SystemKeys.AUTHORITY_PASSWORD);
        }

        try {
            userAuth = authenticationManager.authenticate(userAuth);
        } catch (AccountStatusException ase) {
            // covers expired, locked, disabled cases (mentioned in section 5.2, draft 31)
            throw new UnauthorizedUserException(ase.getMessage());
        } catch (BadCredentialsException e) {
            // If the username/password are wrong the spec says we should send 400/invalid
            // grant
            throw new UnauthorizedUserException(e.getMessage());
        } catch (AuthenticationException e) {
            // any other auth error we send 400/invalid
            throw new UnauthorizedUserException(e.getMessage());
        }
        if (userAuth == null || !userAuth.isAuthenticated()) {
            throw new UnauthorizedUserException("Could not authenticate user: " + username);
        }

        logger.trace("got oauth authentication from security context " + userAuth.toString());

        OAuth2Request storedOAuth2Request = getRequestFactory().createOAuth2Request(client, tokenRequest);
        return new OAuth2Authentication(storedOAuth2Request, userAuth);
    }

    @Override
    protected void validateScope(Set<String> scopes, OAuth2Authentication authentication) {
        if (scopeRegistry != null && clientService != null && userService != null) {
            try {
                UserDetails userDetails = null;
                Authentication userAuth = authentication.getUserAuthentication();
                if (userAuth != null && (userAuth instanceof UserAuthentication)) {
                    userDetails = ((UserAuthentication) userAuth).getUser();
                } else {
                    throw new UnauthorizedUserException("invalid user");
                }

                String clientId = authentication.getOAuth2Request().getClientId();
                it.smartcommunitylab.aac.core.ClientDetails clientDetails = clientService.loadClient(clientId);

                // check each scope is of type user and approved
                for (String s : scopes) {
                    try {
                        Scope scope = scopeRegistry.getScope(s);
                        if (ScopeType.USER != scope.getType() && ScopeType.GENERIC != scope.getType()) {
                            throw new InvalidScopeException("Unauthorized scope: " + s);
                        }

                        ScopeApprover sa = scopeRegistry.getScopeApprover(s);
                        if (sa == null) {
                            // this scope is undecided so skip
                            continue;
                        }

                        Approval approval = sa.approveUserScope(
                            s,
                            translateUser(userDetails, sa.getRealm()),
                            clientDetails,
                            scopes
                        );

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

    private User translateUser(UserDetails userDetails, String realm) {
        if (userService != null) {
            return userService.getUser(userDetails, realm);
        }

        return new User(userDetails);
    }

    public void setClientService(ClientDetailsService clientService) {
        this.clientService = clientService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }
}
