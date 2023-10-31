/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.saml.service;

import it.smartcommunitylab.aac.saml.auth.SerializableSaml2AuthenticationRequestContext;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.codec.CodecPolicy;
import org.apache.commons.codec.binary.Base64;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.provider.service.authentication.AbstractSaml2AuthenticationRequest;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.web.RelyingPartyRegistrationResolver;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.Assert;

public final class Saml2AuthenticationTokenConverter implements AuthenticationConverter {
    private static Base64 BASE64;
    private final Converter<HttpServletRequest, RelyingPartyRegistration> relyingPartyRegistrationResolver;
    private final Function<HttpServletRequest, AbstractSaml2AuthenticationRequest> loader;

    /** @deprecated */
    @Deprecated
    public Saml2AuthenticationTokenConverter(Converter<HttpServletRequest, RelyingPartyRegistration> relyingPartyRegistrationResolver) {
        Assert.notNull(relyingPartyRegistrationResolver, "relyingPartyRegistrationResolver cannot be null");
        this.relyingPartyRegistrationResolver = relyingPartyRegistrationResolver;
        HttpSessionSaml2AuthenticationRequestRepository httpSessionRepository = new HttpSessionSaml2AuthenticationRequestRepository();
        this.loader = (request) -> {
            SerializableSaml2AuthenticationRequestContext requestContext = httpSessionRepository.loadAuthenticationRequest(request);
            if (requestContext == null) {
                return null;
            }
            return requestContext.getSamlAuthenticationRequest();
        };
    }

    public Saml2AuthenticationTokenConverter(RelyingPartyRegistrationResolver relyingPartyRegistrationResolver) {
        this(adaptToConverter(relyingPartyRegistrationResolver));
    }

    private static Converter<HttpServletRequest, RelyingPartyRegistration> adaptToConverter(RelyingPartyRegistrationResolver relyingPartyRegistrationResolver) {
        Assert.notNull(relyingPartyRegistrationResolver, "relyingPartyRegistrationResolver cannot be null");
        return (request) -> {
            return relyingPartyRegistrationResolver.resolve(request, (String)null);
        };
    }

    public Saml2AuthenticationToken convert(HttpServletRequest request) {
        RelyingPartyRegistration relyingPartyRegistration = this.relyingPartyRegistrationResolver.convert(request);
        if (relyingPartyRegistration == null) {
            return null;
        } else {
            String saml2Response = request.getParameter("SAMLResponse");
            if (saml2Response == null) {
                return null;
            } else {
                byte[] b = this.samlDecode(saml2Response);
                saml2Response = this.inflateIfRequired(request, b);
                AbstractSaml2AuthenticationRequest authenticationRequest = this.loadAuthenticationRequest(request);
                return new Saml2AuthenticationToken(relyingPartyRegistration, saml2Response, authenticationRequest);
            }
        }
    }

    private AbstractSaml2AuthenticationRequest loadAuthenticationRequest(HttpServletRequest request) {
        return this.loader.apply(request);
    }

    private String inflateIfRequired(HttpServletRequest request, byte[] b) {
        return HttpMethod.GET.matches(request.getMethod()) ? this.samlInflate(b) : new String(b, StandardCharsets.UTF_8);
    }

    private byte[] samlDecode(String base64EncodedPayload) {
        try {
            return BASE64.decode(base64EncodedPayload);
        } catch (Exception e) {
            throw new Saml2AuthenticationException(new Saml2Error("invalid_response", "Failed to decode SAMLResponse"), e);
        }
    }

    private String samlInflate(byte[] b) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InflaterOutputStream inflaterOutputStream = new InflaterOutputStream(out, new Inflater(true));
            inflaterOutputStream.write(b);
            inflaterOutputStream.finish();
            return out.toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new Saml2AuthenticationException(new Saml2Error("invalid_response", "Unable to inflate string"), e);
        }
    }

    static {
        BASE64 = new Base64(0, new byte[]{10}, false, CodecPolicy.STRICT);
    }
}
