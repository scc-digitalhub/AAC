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

package it.smartcommunitylab.aac.openidfed.auth;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.openidfed.OpenIdFedIdentityAuthority;
import it.smartcommunitylab.aac.openidfed.provider.OpenIdFedIdentityProviderConfig;
import it.smartcommunitylab.aac.openidfed.service.DefaultOpenIdRpMetadataResolver;
import it.smartcommunitylab.aac.openidfed.service.OpenIdRpMetadataResolver;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.lang.Nullable;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

public class OpenIdFedMetadataFilter extends OncePerRequestFilter {

    public static final String OPENID_FEDERATION_URL = Config.WELL_KNOWN_URL + "/openid-federation";

    private static final String MIME_TYPE = "application/entity-statement+jwt";

    public static final String DEFAULT_FILTER_URI = OpenIdFedIdentityAuthority.AUTHORITY_URL + "metadata/{providerId}";

    private final String authorityId;
    private final ProviderConfigRepository<OpenIdFedIdentityProviderConfig> registrationRepository;
    private RequestMatcher requestMatcher;
    private RequestMatcher metaRequestMatcher;
    private RequestMatcher wknRequestMatcher;

    private OpenIdRpMetadataResolver metadataResolver;

    public OpenIdFedMetadataFilter(ProviderConfigRepository<OpenIdFedIdentityProviderConfig> registrationRepository) {
        this(SystemKeys.AUTHORITY_OPENIDFED, registrationRepository, DEFAULT_FILTER_URI);
    }

    public OpenIdFedMetadataFilter(
        String authority,
        ProviderConfigRepository<OpenIdFedIdentityProviderConfig> registrationRepository,
        String filterProcessingUrl
    ) {
        Assert.hasText(authority, "authority can not be null or empty");
        Assert.notNull(registrationRepository, "provider registration repository cannot be null");

        this.authorityId = authority;
        this.registrationRepository = registrationRepository;

        this.metadataResolver = new DefaultOpenIdRpMetadataResolver();

        //build request matcher to serve both metadata and well-known paths
        this.metaRequestMatcher = new AntPathRequestMatcher(filterProcessingUrl, "GET");
        this.wknRequestMatcher = new AntPathRequestMatcher(filterProcessingUrl + OPENID_FEDERATION_URL, "GET");
        this.requestMatcher = new OrRequestMatcher(metaRequestMatcher, wknRequestMatcher);
    }

    public void setMetadataResolver(OpenIdRpMetadataResolver metadataResolver) {
        Assert.notNull(metadataResolver, "metadata resolver cannot be null");
        this.metadataResolver = metadataResolver;
    }

    @Override
    @Nullable
    protected String getFilterName() {
        return getClass().getName() + "." + authorityId;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        RequestMatcher.MatchResult matcher = this.requestMatcher.matcher(request);
        if (!matcher.isMatch()) {
            chain.doFilter(request, response);
            return;
        }

        String providerId = resolveProviderId(request);
        OpenIdFedIdentityProviderConfig config = registrationRepository.findByProviderId(providerId);
        if (config == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            String metadata = metadataResolver.resolveEntityMetadata(config, request);
            writeMetadataToResponse(response, metadata);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void writeMetadataToResponse(HttpServletResponse response, String metadata) throws IOException {
        response.setContentType(MIME_TYPE);
        response.setContentLength(metadata.length());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(metadata);
    }

    private String resolveProviderId(HttpServletRequest request) {
        //try both matchers as fallback, because OR does not extract vars
        RequestMatcher.MatchResult matcher = this.metaRequestMatcher.matcher(request);
        if (!matcher.isMatch()) {
            matcher = this.wknRequestMatcher.matcher(request);
        }

        if (matcher.isMatch()) {
            return matcher.getVariables().get("providerId");
        }

        return null;
    }
}
