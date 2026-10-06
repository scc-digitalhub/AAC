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

package it.smartcommunitylab.aac.eidas.auth;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import net.shibboleth.utilities.java.support.xml.XMLParserException;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.security.impl.SAMLSignatureProfileValidator;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xmlsec.keyinfo.KeyInfoSupport;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.SignatureValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.saml2.core.OpenSamlInitializationService;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/*
 * Verifies the XML signature of the metadata of the eIDAS node before they are trusted.
 *
 * Why here and not in SPID. The SPID technical rules require identity provider metadata to be signed
 * and the Service Provider to verify them; for SPID the check is absorbed by the channel, because the
 * identity providers come from the AgID registry. The eidas provider instead trusts a single url
 * (idpMetadataUrl), and the loader it inherits - Spring Security RelyingPartyRegistrations, through
 * OpenSamlMetadataAssertingPartyDetailsConverter - reads the KeyDescriptors and ignores the Signature
 * element. Whoever can serve that url (or sit on the path to it) can hand us the certificate that will
 * later validate the Responses. This class closes the gap for eidas without touching the SPID module.
 *
 * Two trust models, chosen by configuration:
 *  - a certificate PINNED in the provider configuration (idpMetadataSigningCertificate): the one FICEP
 *    hands out with the node. The signature must verify against it; this is the rule.
 *  - no pinned certificate: the signature is verified against the certificate carried in its own
 *    KeyInfo. This proves integrity and that the signer holds the declared key, not that the key is the
 *    node's: it is logged as a warning and is meant for test and pre-production, where the node
 *    certificate is not distributed out of band.
 *
 * Unsigned metadata are refused in both models. The check runs at provider registration, so a
 * misconfigured or tampered node is rejected there, not at the first login.
 */
public class EIDASMetadataSignatureVerifier {

    private static final Logger logger = LoggerFactory.getLogger(EIDASMetadataSignatureVerifier.class);

    static {
        OpenSamlInitializationService.initialize();
    }

    private EIDASMetadataSignatureVerifier() {}

    /**
     * Parses and verifies the signed metadata.
     *
     * @param metadataXml the metadata document as served by the node
     * @param pinnedCertificate the node certificate in PEM or bare base64, null to verify against the embedded one
     * @return the verified entity descriptor
     * @throws IllegalArgumentException when the metadata cannot be parsed, are unsigned or the signature does not verify
     */
    public static EntityDescriptor verify(String metadataXml, String pinnedCertificate) {
        if (!StringUtils.hasText(metadataXml)) {
            throw new IllegalArgumentException("eIDAS node metadata are empty");
        }

        EntityDescriptor descriptor = parse(metadataXml);
        Signature signature = descriptor.getSignature();
        if (signature == null) {
            throw new IllegalArgumentException(
                "eIDAS node metadata of " + descriptor.getEntityID() + " are not signed: refused"
            );
        }

        try {
            // the SAML profile of XML signature: enveloped, one reference, to the signed element itself
            new SAMLSignatureProfileValidator().validate(signature);

            Credential credential;
            if (StringUtils.hasText(pinnedCertificate)) {
                credential = new BasicX509Credential(parseCertificate(pinnedCertificate));
            } else {
                credential = embeddedCredential(signature, descriptor.getEntityID());
                logger.warn(
                    "eIDAS node metadata of {} verified against the certificate embedded in the signature: " +
                    "integrity only, trust is not pinned. Configure idpMetadataSigningCertificate in production",
                    descriptor.getEntityID()
                );
            }

            SignatureValidator.validate(signature, credential);
        } catch (SignatureException e) {
            throw new IllegalArgumentException(
                "eIDAS node metadata of " + descriptor.getEntityID() + " have an invalid signature: " + e.getMessage(),
                e
            );
        } catch (CertificateException e) {
            throw new IllegalArgumentException("invalid certificate for eIDAS node metadata: " + e.getMessage(), e);
        }

        logger.debug("eIDAS node metadata of {} verified", descriptor.getEntityID());
        return descriptor;
    }

    private static EntityDescriptor parse(String metadataXml) {
        try {
            Element element = XMLObjectProviderRegistrySupport
                .getParserPool()
                .parse(new StringReader(metadataXml))
                .getDocumentElement();
            XMLObject object = XMLObjectSupport.getUnmarshaller(element).unmarshall(element);
            if (!(object instanceof EntityDescriptor)) {
                throw new IllegalArgumentException(
                    "eIDAS node metadata are not an EntityDescriptor: " + object.getElementQName()
                );
            }
            return (EntityDescriptor) object;
        } catch (XMLParserException | UnmarshallingException e) {
            throw new IllegalArgumentException("eIDAS node metadata cannot be parsed: " + e.getMessage(), e);
        }
    }

    private static Credential embeddedCredential(Signature signature, String entityId) throws CertificateException {
        if (signature.getKeyInfo() == null) {
            throw new IllegalArgumentException(
                "eIDAS node metadata of " + entityId + " are signed without KeyInfo and no certificate is pinned"
            );
        }
        List<X509Certificate> certificates = KeyInfoSupport.getCertificates(signature.getKeyInfo());
        if (certificates.isEmpty()) {
            throw new IllegalArgumentException(
                "eIDAS node metadata of " + entityId + " carry no certificate in the signature and none is pinned"
            );
        }
        return new BasicX509Credential(certificates.get(0));
    }

    /* PEM with or without armor, whitespace tolerant */
    static X509Certificate parseCertificate(String pem) throws CertificateException {
        String base64 = pem
            .replaceAll("-----BEGIN CERTIFICATE-----", "")
            .replaceAll("-----END CERTIFICATE-----", "")
            .replaceAll("\\s", "");
        byte[] der = Base64.getMimeDecoder().decode(base64.getBytes(StandardCharsets.US_ASCII));
        return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(der));
    }
}
