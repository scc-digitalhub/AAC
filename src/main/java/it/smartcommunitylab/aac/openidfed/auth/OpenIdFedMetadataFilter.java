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

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.openidfed.OpenIdFedIdentityAuthority;
import it.smartcommunitylab.aac.openidfed.provider.OpenIdFedIdentityProviderConfig;
import it.smartcommunitylab.aac.openidfed.service.OpenIdFedMetadataResolver;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.lang.Nullable;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

public class OpenIdFedMetadataFilter extends OncePerRequestFilter {

    private static final String MIME_TYPE = "application/entity-statement+jwt";

    public static final String DEFAULT_FILTER_URI =
        OpenIdFedIdentityAuthority.AUTHORITY_URL + "metadata/{registrationId}";

    private final String authorityId;
    private final ProviderConfigRepository<OpenIdFedIdentityProviderConfig> registrationRepository;
    private RequestMatcher requestMatcher;
    private OpenIdFedMetadataResolver metadataResolver;

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

        this.metadataResolver = new OpenIdFedMetadataResolver();
        this.requestMatcher = new AntPathRequestMatcher(filterProcessingUrl, "GET");
    }

    public void setMetadataResolver(OpenIdFedMetadataResolver metadataResolver) {
        Assert.notNull(metadataResolver, "metadata resolver cannot be null");
        this.metadataResolver = metadataResolver;
    }

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

        String providerId = matcher.getVariables().get("registrationId");
        OpenIdFedIdentityProviderConfig config = registrationRepository.findByProviderId(providerId);
        if (config == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            String metadata = metadataResolver.resolveRpMetadata(config, request);
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
}
