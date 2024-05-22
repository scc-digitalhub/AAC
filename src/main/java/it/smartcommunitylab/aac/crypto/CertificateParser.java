package it.smartcommunitylab.aac.crypto;

import java.io.IOException;
import java.io.StringReader;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.regex.Pattern;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.util.StringUtils;

public class CertificateParser {

    private static final Pattern headPat = Pattern.compile("^-----BEGIN [A-Z]*-----$");
    private static final Pattern footPat = Pattern.compile("^-----END [A-Z]*-----$");
    private static final String headFmt = "-----BEGIN %s-----";
    private static final String footFmt = "-----END %s-----";

    // withoutHeader verify if the given PEM is missing either the header
    // or the footer (or both)
    private static boolean withoutHeader(String pem) {
        if (!StringUtils.hasText(pem)) {
            return true;
        }
        String[] lines = pem.split("\\R");
        if (lines.length > 2) {
            String headerLine = lines[0];
            String footerLine = lines[lines.length - 1];
            return headPat.matcher(headerLine).find() && footPat.matcher(footerLine).find();
        }
        return true;
    }

    // addHeader inserts a header and a footer of the given kind to a PEM
    // The header is ALWAYS inserted, even when it's already present; as such
    //  usage should often be accompanied by withoutHeader function
    private static String addHeader(String pem, String kind) {
        String header = String.format(headFmt, kind);
        String footer = String.format(footFmt, kind);
        return header + "\n" + pem + "\n" + footer;
    }

    // verifyHeader checks if the given pem has an header of the given kind
    private static boolean checkHeader(String pem, String kind) {
        String header = String.format(headFmt, kind);
        String footer = String.format(footFmt, kind);
        String[] lines = pem.split("\\R");
        if (lines.length > 2) {
            String headerLine = lines[0];
            String footerLine = lines[lines.length - 1];
            return (headerLine.equals(header) && footerLine.equals(footer));
        }
        // header is missing: for sure it's wrong
        return false;
    }

    private static PrivateKey parsePrivateKeyStrict(String key) throws IOException {
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

    private static PrivateKey parsePrivateKey(String keyPem) throws IOException {
        PrivateKey pk = null;
        if (withoutHeader(keyPem)) {
            // header must be guessed: try RSA first, then generic
            try {
                pk = parsePrivateKeyStrict(addHeader(keyPem, "RSA PRIVATE KEY"));
            } catch (IllegalArgumentException | IOException e) {
                pk = parsePrivateKeyStrict(addHeader(keyPem, "PRIVATE KEY"));
            }
            return pk;
        }
        pk = parsePrivateKeyStrict(keyPem);
        return pk;
    }

    private static X509Certificate parseX509(String source) throws IOException, CertificateException {
        // if source is missing header, try to guess it in order to parse as X509 certificate
        String pemSrc;
        if (withoutHeader(source)) {
            pemSrc = addHeader(source, "CERTIFICATE");
        } else {
            pemSrc = source;
        }
        if (!checkHeader(source, "CERTIFICATE")) {
            throw new IllegalArgumentException("invalid certificate");
        }

        StringReader sr = new StringReader(pemSrc);
        PEMParser pr = new PEMParser(sr);
        JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
        Object pem = pr.readObject();
        sr.close();

        if (pem instanceof X509CertificateHolder) {
            return converter.getCertificate((X509CertificateHolder) pem);
        }
        throw new IllegalArgumentException("invalid certificate");
    }

    public static Saml2X509Credential genCredentials(
        String key,
        String certificate,
        Saml2X509Credential.Saml2X509CredentialType... keyUse
    ) throws IOException, CertificateException {
        PrivateKey pk = CertificateParser.parsePrivateKey(key);
        X509Certificate cert = CertificateParser.parseX509(certificate);
        return new Saml2X509Credential(pk, cert, keyUse);
    }
}
