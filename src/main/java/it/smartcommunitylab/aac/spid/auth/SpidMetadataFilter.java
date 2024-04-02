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
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.RelyingPartyRegistrationResolver;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

public class SpidMetadataFilter extends OncePerRequestFilter {

    // TODO: check specs if this URI is ok
    public static final String DEFAULT_FILTER_URI = SpidIdentityAuthority.AUTHORITY_URL + "metadata/{registrationId}";

    private final RequestMatcher requestMatcher;
    private final RelyingPartyRegistrationResolver registrationResolver;
    private final SpidMetadataResolver metadataResolver;

    public SpidMetadataFilter(
        ProviderConfigRepository<SpidIdentityProviderConfig> configRepository,
        RelyingPartyRegistrationRepository relyingPartyRegistrationRepository
    ) {
        Assert.notNull(configRepository, "provider registration repository cannot be null");
        Assert.notNull(relyingPartyRegistrationRepository, "relyingPartyRegistrationRepository cannot be null");
        this.registrationResolver = new DefaultRelyingPartyRegistrationResolver(relyingPartyRegistrationRepository);

        requestMatcher = new AntPathRequestMatcher(DEFAULT_FILTER_URI);
        metadataResolver = new SpidMetadataResolver(configRepository);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        RequestMatcher.MatchResult matcher = this.requestMatcher.matcher(request);
        if (!matcher.isMatch()) {
            // not a request toward a metadata endpoint: move on
            filterChain.doFilter(request, response);
            return;
        }
        // fetch registration
        String registrationId = matcher.getVariables().get("registrationId");
        RelyingPartyRegistration relyingPartyRegistration = registrationResolver.resolve(request, registrationId);
        if (relyingPartyRegistration == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        // generate metadata xml
        String metadata = metadataResolver.resolve(relyingPartyRegistration);
        // write response
        response.setContentType(MediaType.APPLICATION_XML_VALUE);
        //        response.setHeader(
        //            HttpHeaders.CONTENT_DISPOSITION,
        //            "attachment; filename=\"spid-" + registrationId + "-metadata.xml\""
        //        );
        response.setContentLength(metadata.length());
        response.getWriter().write(metadata);
    }
}
