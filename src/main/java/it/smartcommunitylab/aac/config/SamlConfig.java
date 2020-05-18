package it.smartcommunitylab.aac.config;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.saml.key.JKSKeyManager;
import org.springframework.security.saml.key.KeyManager;

@Configuration
public class SamlConfig {

    @Value("${saml.keystore.location}")
    private Resource location;

    @Value("${saml.keystore.password}")
    private String password;

    @Value("${saml.keystore.alias}")
    private String alias;

    public KeyStore getKeyStore()
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {

        // get default ks instance from builder
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

        // Load KeyStore from input stream
//            InputStream keystoreInputStream = getClass()
//                    .getResourceAsStream("classpath:/saml/star_smartcommunitylab_it.p12");

        // load via resource
        ks.load(location.getInputStream(), password.toCharArray());
        return ks;

    }

    @Bean
    public KeyManager samlKeyManager()
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {

        // define keystore passwords
        Map<String, String> passwords = new HashMap<String, String>();
        passwords.put(alias, password);
        String defaultKey = alias;

        // load manager from keystore
        KeyManager keyManager = new JKSKeyManager(getKeyStore(), passwords, defaultKey);
        return keyManager;
    }

}
