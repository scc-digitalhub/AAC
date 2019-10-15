/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.aac.config;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;

import it.smartcommunitylab.aac.openid.service.DefaultJWTEncryptionAndDecryptionService;
import it.smartcommunitylab.aac.openid.service.DefaultJWTSigningAndValidationService;
import it.smartcommunitylab.aac.openid.service.JWKSetKeyStore;
import it.smartcommunitylab.aac.openid.service.JWTEncryptionAndDecryptionService;
import it.smartcommunitylab.aac.openid.service.JWTSigningAndValidationService;

/**
 * @author raman
 *
 */
@Configuration
public class OpenIDConfig {

    @Value("${openid.keystore}")
	private Resource location;
	
	@Bean()
	public JWTSigningAndValidationService getJWTSigningAndValidationService() throws NoSuchAlgorithmException, InvalidKeySpecException {
		DefaultJWTSigningAndValidationService service = new DefaultJWTSigningAndValidationService(getJWKSetKeyStore());
		service.setDefaultSignerKeyId("rsa1");
		service.setDefaultSigningAlgorithmName("RS256");
		return service;
	}

	@Bean()
	@Primary
	public JWKSetKeyStore getJWKSetKeyStore() {
		JWKSetKeyStore keystore = new JWKSetKeyStore();
		keystore.setLocation(location);
		return keystore;
	}
	
	@Bean()
	public JWTEncryptionAndDecryptionService getJWTEncryptionAndDecryptionService() throws JOSEException, NoSuchAlgorithmException, InvalidKeySpecException {
		DefaultJWTEncryptionAndDecryptionService service = new DefaultJWTEncryptionAndDecryptionService(getJWKSetKeyStore());
		service.setDefaultDecryptionKeyId("rsa1");
		service.setDefaultEncryptionKeyId("rsa1");
		service.setDefaultAlgorithm(JWEAlgorithm.RSA_OAEP_256);
		return service;
	}

}
