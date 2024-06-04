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

package it.smartcommunitylab.aac.spid.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;
import org.opensaml.core.config.ConfigurationService;
import org.opensaml.core.xml.config.XMLObjectProviderRegistry;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.impl.AuthnRequestUnmarshaller;
import org.springframework.lang.Nullable;
import org.springframework.security.saml2.Saml2Exception;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.core.Saml2ErrorCodes;
import org.springframework.security.saml2.provider.service.authentication.AbstractSaml2AuthenticationRequest;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SpidRequestParser {

    private static final AuthnRequestUnmarshaller authnRequestUnmarshaller;

    static {
        XMLObjectProviderRegistry registry = ConfigurationService.get(XMLObjectProviderRegistry.class);
        authnRequestUnmarshaller =
            (AuthnRequestUnmarshaller) registry
                .getUnmarshallerFactory()
                .getUnmarshaller(AuthnRequest.DEFAULT_ELEMENT_NAME);
    }

    public static @Nullable AuthnRequest parse(AbstractSaml2AuthenticationRequest request) {
        if (request == null) {
            return null;
        }
        String samlRequest = request.getSamlRequest();
        if (!StringUtils.hasText(samlRequest)) {
            return null;
        }
        if (request.getBinding() == Saml2MessageBinding.REDIRECT) {
            samlRequest = ParserUtils.inflate(ParserUtils.decode(samlRequest));
        } else {
            samlRequest = new String(ParserUtils.decode(samlRequest), StandardCharsets.UTF_8);
        }
        try {
            Document document = XMLObjectProviderRegistrySupport
                .getParserPool()
                .parse(new ByteArrayInputStream(samlRequest.getBytes(StandardCharsets.UTF_8)));
            Element element = document.getDocumentElement();
            return (AuthnRequest) authnRequestUnmarshaller.unmarshall(element);
        } catch (Exception ex) {
            String message = "Failed to deserialize associated authentication request [" + ex.getMessage() + "]";
            throw new Saml2AuthenticationException(new Saml2Error(Saml2ErrorCodes.MALFORMED_REQUEST_DATA, message), ex);
        }
    }

    public static class ParserUtils {

        public static String inflate(byte[] b) {
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                InflaterOutputStream iout = new InflaterOutputStream(out, new Inflater(true));
                iout.write(b);
                iout.finish();
                return out.toString(StandardCharsets.UTF_8);
            } catch (IOException ex) {
                throw new Saml2Exception("Unable to inflate string", ex);
            }
        }

        public static byte[] decode(String s) {
            return Base64.getMimeDecoder().decode(s);
        }
    }
}
