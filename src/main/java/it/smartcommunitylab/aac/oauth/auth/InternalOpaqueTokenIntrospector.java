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

package it.smartcommunitylab.aac.oauth.auth;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.api.scopes.ApiScopeProvider;
import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.oauth.AACOAuth2AccessToken;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.util.Assert;

/*
 * A token inspector which resolves by looking via tokenStore.
 * By leveraging subject service the resulting principal will have up-to-date authorities.
 */

public class InternalOpaqueTokenIntrospector implements OpaqueTokenIntrospector {

    private final TokenStore tokenStore;

    private SubjectService subjectService;

    private ApiScopeProvider apiProvider = new ApiScopeProvider();

    public InternalOpaqueTokenIntrospector(TokenStore tokenStore) {
        Assert.notNull(tokenStore, "token store can not be null");
        this.tokenStore = tokenStore;
    }

    public void setApiProvider(ApiScopeProvider apiProvider) {
        this.apiProvider = apiProvider;
    }

    public void setSubjectService(SubjectService subjectService) {
        this.subjectService = subjectService;
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
            String subjectId = accessToken.getSubject();

            // make sure subject is valid
            Subject subject = subjectService.getSubject(subjectId);

            // principal is the subject, which is the entity issuing the token
            String principal = subjectId;
            Set<GrantedAuthority> authorities = new HashSet<>();

            // add base authorities - workaround
            // TODO fetch from service
            if (subjectId.equals(accessToken.getAuthorizedParty())) {
                // client_credentials
                authorities.add(new SimpleGrantedAuthority(Config.R_CLIENT));
            } else {
                authorities.add(new SimpleGrantedAuthority(Config.R_USER));
            }

            // add scopes as authorities
            for (String scope : token.getScope()) {
                authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
            }

            // we need a way to discover which authorities are delegated via the token,
            // for example via scopes. For now we add all roles and use scopes to check
            // note this is ONLY for core API access, where we explicitly check for scopes
            Collection<GrantedAuthority> subjectAuthorities = subjectService.getAuthorities(subjectId);
            authorities.addAll(subjectAuthorities);

            Map<String, Object> params = new HashMap<>();
            params.put("sub", accessToken.getSubject());
            params.put("scopes", token.getScope());
            params.put("realm", realm);
            params.putAll(accessToken.getClaims());

            return new DefaultOAuth2AuthenticatedPrincipal(realm, principal, params, authorities);
        } catch (NoSuchSubjectException e) {
            throw new BadOpaqueTokenException("Provided token isn't active");
        }
    }
}
