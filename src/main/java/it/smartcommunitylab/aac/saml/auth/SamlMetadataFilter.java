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

package it.smartcommunitylab.aac.saml.auth;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.saml.SamlIdentityAuthority;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.security.saml2.provider.service.metadata.OpenSamlMetadataResolver;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.Saml2MetadataFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

public class SamlMetadataFilter extends OncePerRequestFilter {

    public static final String DEFAULT_FILTER_URI = SamlIdentityAuthority.AUTHORITY_URL + "metadata/{registrationId}";

    private final String authorityId;
    private final Saml2MetadataFilter samlMetadataFilter;

    public SamlMetadataFilter(RelyingPartyRegistrationRepository relyingPartyRegistrationRepository) {
        this(SystemKeys.AUTHORITY_SAML, relyingPartyRegistrationRepository, DEFAULT_FILTER_URI);
    }

    public SamlMetadataFilter(
        String authority,
        RelyingPartyRegistrationRepository relyingPartyRegistrationRepository,
        String filterProcessingUrl
    ) {
        Assert.hasText(authority, "authority can not be null or empty");

        this.authorityId = authority;

        // build a converter and a resolver for the filter
        Converter<HttpServletRequest, RelyingPartyRegistration> relyingPartyRegistrationResolver =
            new DefaultRelyingPartyRegistrationResolver(relyingPartyRegistrationRepository);

        samlMetadataFilter = new Saml2MetadataFilter(relyingPartyRegistrationResolver, new OpenSamlMetadataResolver());

        RequestMatcher requestMatcher = new AntPathRequestMatcher(filterProcessingUrl, "GET");
        samlMetadataFilter.setRequestMatcher(requestMatcher);
    }

    @Nullable
    protected String getFilterName() {
        return getClass().getName() + "." + authorityId;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        // delegate
        samlMetadataFilter.doFilter(request, response, filterChain);
    }
}
