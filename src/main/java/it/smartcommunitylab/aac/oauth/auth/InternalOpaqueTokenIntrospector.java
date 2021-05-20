package it.smartcommunitylab.aac.oauth.auth;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.api.scopes.ApiScope;
import it.smartcommunitylab.aac.api.scopes.ApiScopeProvider;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.auth.RealmGrantedAuthority;
import it.smartcommunitylab.aac.core.persistence.ClientEntity;
import it.smartcommunitylab.aac.core.persistence.ClientRoleEntity;
import it.smartcommunitylab.aac.core.persistence.UserEntity;
import it.smartcommunitylab.aac.core.persistence.UserRoleEntity;
import it.smartcommunitylab.aac.core.service.ClientEntityService;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.oauth.AACOAuth2AccessToken;

/*
 * A token inspector which resolves by looking via tokenStore.
 * By leveraging user and client services the resulting principal will have up-to-date authorities.
 */

public class InternalOpaqueTokenIntrospector implements OpaqueTokenIntrospector {

    private final TokenStore tokenStore;
    private UserEntityService userService;
    private ClientEntityService clientService;

    private ApiScopeProvider apiProvider = new ApiScopeProvider();

    public InternalOpaqueTokenIntrospector(
            TokenStore tokenStore) {
        Assert.notNull(tokenStore, "token store can not be null");
        this.tokenStore = tokenStore;
    }

    public void setUserService(UserEntityService userService) {
        this.userService = userService;
    }

    public void setClientService(ClientEntityService clientService) {
        this.clientService = clientService;
    }

    public void setApiProvider(ApiScopeProvider apiProvider) {
        this.apiProvider = apiProvider;
    }

    public OAuth2AuthenticatedPrincipal introspect(String tokenValue) {

        OAuth2AccessToken token = tokenStore.readAccessToken(tokenValue);
        if (token == null) {
            throw new BadOpaqueTokenException("Provided token isn't active");
        }

        if (token.isExpired()) {
            throw new BadOpaqueTokenException("Provided token isn't active");
        }

        // we support only our tokens, we need full info
        if (!(token instanceof AACOAuth2AccessToken)) {
            throw new BadOpaqueTokenException("Provided token isn't active");
        }

        AACOAuth2AccessToken accessToken = (AACOAuth2AccessToken) token;
        String realm = accessToken.getRealm();

        OAuth2Authentication auth = tokenStore.readAuthentication(tokenValue);
        if (auth == null) {
            throw new BadOpaqueTokenException("Provided token isn't active");
        }

        try {
            // principal is the authorized party, since we can't know who is the token
            // carrier
            String principal = accessToken.getAuthorizedParty();
            if (!StringUtils.hasText(principal)) {
                // fallback to subject, which is the entity issuing the token
                principal = accessToken.getSubject();
            }

            Set<GrantedAuthority> authorities = new HashSet<>();

            // add scopes as authorities
            for (String scope : token.getScope()) {
                authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
            }

            // we need a way to discover which authorities are delegated via the token,
            // for example via scopes. For now we add all roles and use scopes to check
            // note this is ONLY for core API access, where we explicitly check for scopes

            Authentication userAuth = auth.getUserAuthentication();
            if (userAuth != null && userAuth.getName().equals(accessToken.getSubject())) {
                // user grant, if user exists we delegate authorities
                // we fetch again authorities for the given user
                String subjectId = userAuth.getName();
                principal = subjectId;

                if (userService != null) {
                    List<UserRoleEntity> roles = userService.getRoles(subjectId);

                    // translate to authorities
                    authorities.add(new SimpleGrantedAuthority(Config.R_USER));
                    for (UserRoleEntity role : roles) {
                        if (StringUtils.hasText(role.getRealm())) {
                            authorities.add(new RealmGrantedAuthority(role.getRealm(), role.getRole()));
                        } else {
                            authorities.add(new SimpleGrantedAuthority(role.getRole()));
                        }
                    }
                } else {
                    // use stale info from saved token
                    authorities.addAll(userAuth.getAuthorities());
                }

            } else if (auth.getName().equals(accessToken.getSubject())
                    && accessToken.getSubject().equals(accessToken.getAuthorizedParty())) {
                // self issued, ie client credentials
                String clientId = auth.getName();
                if (clientService != null) {
                    List<ClientRoleEntity> roles = clientService.getRoles(clientId);

                    // translate to authorities
                    authorities.add(new SimpleGrantedAuthority(Config.R_CLIENT));
                    for (ClientRoleEntity role : roles) {
                        if (SystemKeys.REALM_GLOBAL.equals(role.getRealm())) {
                            authorities.add(new SimpleGrantedAuthority(role.getRole()));
                        } else {
                            authorities.add(new RealmGrantedAuthority(role.getRealm(), role.getRole()));
                        }
                    }
                } else {
                    // use stale info from saved token
                    authorities.addAll(auth.getAuthorities());
                }
            }

            Map<String, Object> params = new HashMap<>();
            params.put("sub", accessToken.getSubject());
            params.put("scopes", token.getScope());

            return new DefaultOAuth2AuthenticatedPrincipal(
                    principal, params, authorities);

        } catch (NoSuchClientException | NoSuchUserException e) {
            throw new BadOpaqueTokenException("Provided token isn't active");
        }

    }

}