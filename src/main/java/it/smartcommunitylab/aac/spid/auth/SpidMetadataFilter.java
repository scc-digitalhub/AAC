/*
 * Copyright 2024 the original author or authors
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

package it.smartcommunitylab.aac.spid.auth;

import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.spid.SpidIdentityAuthority;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfig;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.RelyingPartyRegistrationResolver;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

public class SpidMetadataFilter extends OncePerRequestFilter {

    public static final String DEFAULT_FILTER_URI = SpidIdentityAuthority.AUTHORITY_URL + "metadata/{registrationId}";
    public static final String DEFAULT_METADATA_FILE_NAME = "spid-{registrationId}-metadata.xml";

    private final RelyingPartyRegistrationResolver relyingPartyRegistrationResolver;
	private final SpidMetadataResolver metadataResolver;
    private final String filterProcessingUrl;
    private final RequestMatcher requestMatcher;
    private String metadataFilename = DEFAULT_METADATA_FILE_NAME;

    public SpidMetadataFilter(
        ProviderConfigRepository<SpidIdentityProviderConfig> configRepository,
        RelyingPartyRegistrationRepository relyingPartyRegistrationRepository
    ) {
        this(configRepository, relyingPartyRegistrationRepository, DEFAULT_FILTER_URI);
    }

    public SpidMetadataFilter(
        ProviderConfigRepository<SpidIdentityProviderConfig> configRepository,
        RelyingPartyRegistrationRepository relyingPartyRegistrationRepository,
        String filterProcessingUrl
    ) {
        Assert.notNull(configRepository, "provider registration repository cannot be null");
        Assert.notNull(relyingPartyRegistrationRepository, "relyingPartyRegistrationRepository cannot be null");

        this.relyingPartyRegistrationResolver = new DefaultRelyingPartyRegistrationResolver(
            relyingPartyRegistrationRepository
        );
        this.metadataResolver = new SpidMetadataResolver(configRepository);
        this.filterProcessingUrl = filterProcessingUrl;
        this.requestMatcher = new AntPathRequestMatcher(filterProcessingUrl);
    }

    /*
     * OncePerRequestFilter tells instances apart by name: the filter is instantiated once per
     * authority built on the SPID stack (spid, eidas...), so the name carries the endpoint.
     */
    @Nullable
    protected String getFilterName() {
        return getClass().getName() + "." + filterProcessingUrl;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        RequestMatcher.MatchResult matcher = this.requestMatcher.matcher(request);
		if (!matcher.isMatch()) {
			filterChain.doFilter(request, response);
			return;
		}
		String registrationId = matcher.getVariables().get("registrationId");
		RelyingPartyRegistration relyingPartyRegistration = relyingPartyRegistrationResolver.resolve(request, registrationId);
		if (relyingPartyRegistration == null) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}
		String metadata = metadataResolver.resolve(relyingPartyRegistration);
		writeMetadataToResponse(response, relyingPartyRegistration.getRegistrationId(), metadata);
    }

    private void writeMetadataToResponse(HttpServletResponse response, String registrationId, String metadata) 
            throws IOException {
        response.setContentType(MediaType.APPLICATION_XML_VALUE);
		String fileName = metadataFilename.replace("{registrationId}", registrationId);
		String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name());
		String format = "attachment; filename=\"%s\"; filename*=UTF-8''%s";
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION, String.format(format, fileName, encodedFileName));
		response.setContentLength(metadata.getBytes(StandardCharsets.UTF_8).length);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.getWriter().write(metadata);
	}
}
