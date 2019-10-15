package it.smartcommunitylab.aac.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import it.smartcommunitylab.aac.openid.service.JWKSetKeyStore;

/**
 * @author matteo
 *
 */
@Configuration
public class OauthConfig {

    @Value("${oauth2.keystore}")
    private Resource location;

    @Bean(name = "oauthJWKSetKeyStore")
    public JWKSetKeyStore getJWKSetKeyStore() {
        JWKSetKeyStore keystore = new JWKSetKeyStore();
        keystore.setLocation(location);
        return keystore;
    }

}