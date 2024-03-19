package it.smartcommunitylab.aac.crypto;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.security.saml2.core.Saml2X509Credential;

import java.io.IOException;
import java.io.StringReader;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class CertificateParser {

    // updatePemHeaderFooter ensured that a PEM (as UTF-8 string) starts
    // and terminated with the appropriate footer. Which footer is appropriate
    // depends in the the params kind
    // TODO: kind should be enum
    private static String updatePemHeaderFooter(String value, String kind) {
        String sep = "-----";
        String begin = "BEGIN " + kind;
        String end = "END " + kind;

        String header = sep + begin + sep;
        String footer = sep + end + sep;

        String[] lines = value.split("\\R");

        if (lines.length > 2) {
            // headers?
            String headerLine = lines[0];
            String footerLine = lines[lines.length - 1];

            if (headerLine.startsWith(sep) && footerLine.startsWith(sep)) {
                // return unchanged, don't mess with content
                return value;
            }
        }

        // rewrite
        StringBuilder sb = new StringBuilder();
        sb.append(header).append("\n");
        for (String line : lines) {
            sb.append(line.trim()).append("\n");
        }
        sb.append(footer);
        return sb.toString();
    }

    public static X509Certificate parseX509(String source) throws IOException, CertificateException {
        String src = updatePemHeaderFooter(source, "CERTIFICATE");
        StringReader sr = new StringReader(src);
        PEMParser pr = new PEMParser(sr);
        JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
        Object pem = pr.readObject();
        sr.close();

        if (pem instanceof X509CertificateHolder) {
            return converter.getCertificate((X509CertificateHolder) pem);
        }
        throw new IllegalArgumentException("invalid certificate");
    }

    public static PrivateKey parsePrivateKey(String key) throws IOException {
        StringReader sr = new StringReader(key);
        PEMParser pr = new PEMParser(sr);
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        Object pem = pr.readObject();
        sr.close();

        if (pem instanceof PEMKeyPair) {
            PrivateKeyInfo privateKeyInfo = ((PEMKeyPair) pem).getPrivateKeyInfo();
            return converter.getPrivateKey(privateKeyInfo);
        } else if (pem instanceof PrivateKeyInfo) {
            return converter.getPrivateKey((PrivateKeyInfo) pem);
        }

        throw new IllegalArgumentException("invalid private key");
    }

    public static PrivateKey parsePrivateWithUndefinedHeader(String key) throws IOException {
        // first try as rsa
        PrivateKey pk = null;
        try {
            pk = parsePrivateKey(updatePemHeaderFooter(key, "RSA PRIVATE KEY"));
        } catch (IllegalArgumentException | IOException e) {
            // fallback as private
            pk = parsePrivateKey(updatePemHeaderFooter(key, "PRIVATE KEY"));
        }

        return pk;
    }

    public static Saml2X509Credential genCredentials(String key, String certificate, Saml2X509Credential.Saml2X509CredentialType... keyUse)
            throws IOException, CertificateException {
        PrivateKey pk = CertificateParser.parsePrivateWithUndefinedHeader(key);
        X509Certificate cert = CertificateParser.parseX509(certificate);
        return new Saml2X509Credential(pk, cert, keyUse);
    }
}
