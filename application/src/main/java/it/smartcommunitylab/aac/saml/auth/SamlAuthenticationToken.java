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
import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.util.Assert;

public class SamlAuthenticationToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = SystemKeys.AAC_SAML_SERIAL_VERSION;

    private final String subject;

    private final Saml2AuthenticatedPrincipal principal;

    private final String saml2Response;

    //    private final transient ResponseToken responseToken;

    public SamlAuthenticationToken(
        String subject,
        Saml2AuthenticatedPrincipal principal,
        String saml2Response,
        Collection<? extends GrantedAuthority> authorities
    ) {
        super(authorities);
        Assert.hasText(subject, "subject cannot be null or empty");
        Assert.notNull(principal, "principal cannot be null");
        Assert.hasText(saml2Response, "saml2Response cannot be null or empty");
        this.subject = subject;
        this.principal = principal;
        this.saml2Response = saml2Response;
        //        this.saml2Response = responseToken.getToken().getSaml2Response();
        //        this.responseToken = responseToken;
        setAuthenticated(true);
    }

    @Override
    public AuthenticatedPrincipal getPrincipal() {
        return this.principal;
    }

    @Override
    public Object getCredentials() {
        return getSaml2Response();
    }

    public String getSubject() {
        return subject;
    }

    //    @JsonIgnore
    //    public ResponseToken getResponseToken() {
    //        return responseToken;
    //    }

    public String getSaml2Response() {
        return saml2Response;
    }
}
