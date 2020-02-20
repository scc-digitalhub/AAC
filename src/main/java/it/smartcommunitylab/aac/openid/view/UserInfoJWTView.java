/*******************************************************************************
 * Copyright 2018 The MIT Internet Trust Consortium
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package it.smartcommunitylab.aac.openid.view;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import it.smartcommunitylab.aac.jwt.ClientKeyCacheService;
import it.smartcommunitylab.aac.jwt.JWTEncryptionAndDecryptionService;
import it.smartcommunitylab.aac.jwt.JWTSigningAndValidationService;
import it.smartcommunitylab.aac.jwt.SymmetricKeyJWTValidatorCacheService;
import it.smartcommunitylab.aac.manager.ServiceManager;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;


/**
 * @author jricher
 *
 */
@Component(UserInfoJWTView.VIEWNAME)
public class UserInfoJWTView extends UserInfoView {
    private final Logger logger = LoggerFactory.getLogger(getClass());

	public static final String VIEWNAME = "userInfoJwtView";

	public static final String JOSE_MEDIA_TYPE_VALUE = "application/jwt";
	public static final MediaType JOSE_MEDIA_TYPE = new MediaType("application", "jwt");

	@Value("${jwt.issuer}")
	private String issuer;

	@Autowired
	private JWTSigningAndValidationService jwtService;

	@Autowired
	private ClientKeyCacheService encrypters;

	@Autowired
	private SymmetricKeyJWTValidatorCacheService symmetricCacheService;

	@Autowired
    private ServiceManager serviceManager;

	@SuppressWarnings("unchecked")
	@Override
	protected void writeOut(Map<String, Object> json, Map<String, Object> model,
			HttpServletRequest request, HttpServletResponse response) {

		try {
			ClientDetailsEntity client = (ClientDetailsEntity)model.get(CLIENT);

			// use the parser to import the user claims into the object
			StringWriter writer = new StringWriter();
			gson.toJson(json, writer);

			Set<String> scope = (Set<String>) model.get(SCOPE);
	        List<String> audiences = new LinkedList<>();
	        audiences.add(client.getClientId());
	        audiences.addAll(getServiceIds(scope));

			response.setContentType(JOSE_MEDIA_TYPE_VALUE);

			JWTClaimsSet claims = new JWTClaimsSet.Builder(JWTClaimsSet.parse(writer.toString()))
					.audience(audiences)
					.claim("azp", client.getClientId())
					.issuer(issuer)
					.issueTime(new Date())
					.jwtID(UUID.randomUUID().toString()) // set a random NONCE in the middle of it
					.build();

			JWSAlgorithm signedResponseAlg = ClientKeyCacheService.getSignedResponseAlg(client);
			JWEAlgorithm encResponseAlg = ClientKeyCacheService.getEncryptedResponseAlg(client);
			EncryptionMethod encResponseEnc = ClientKeyCacheService.getEncryptedResponseEnc(client);
			String jwksUri = ClientKeyCacheService.getJwksUri(client);
			JWKSet jwks = ClientKeyCacheService.getJwks(client);

			if (encResponseAlg != null && !encResponseAlg.equals(Algorithm.NONE)
					&& encResponseEnc != null && !encResponseEnc.equals(Algorithm.NONE)
					&& (!Strings.isNullOrEmpty(jwksUri) || jwks != null)) {

				// encrypt it to the client's key

				JWTEncryptionAndDecryptionService encrypter = encrypters.getEncrypter(client);

				if (encrypter != null) {

					EncryptedJWT encrypted = new EncryptedJWT(new JWEHeader(encResponseAlg, encResponseEnc), claims);

					encrypter.encryptJwt(encrypted);


					Writer out = response.getWriter();
					out.write(encrypted.serialize());

				} else {
					logger.error("Couldn't find encrypter for client: " + client.getClientId());
				}
			} else {

				JWSAlgorithm signingAlg = jwtService.getDefaultSigningAlgorithm(); // default to the server's preference
				if (signedResponseAlg != null) {
					signingAlg = signedResponseAlg; // override with the client's preference if available
				}
				JWSHeader header = new JWSHeader(signingAlg, null, null, null, null, null, null, null, null, null,
						jwtService.getDefaultSignerKeyId(),
						null, null);
				SignedJWT signed = new SignedJWT(header, claims);

				if (signingAlg.equals(JWSAlgorithm.HS256)
						|| signingAlg.equals(JWSAlgorithm.HS384)
						|| signingAlg.equals(JWSAlgorithm.HS512)) {

					// sign it with the client's secret
					JWTSigningAndValidationService signer = symmetricCacheService.getSymmetricValidator(client);
					signer.signJwt(signed);

				} else {
					// sign it with the server's key
					jwtService.signJwt(signed);
				}

				Writer out = response.getWriter();
				out.write(signed.serialize());
			}
		} catch (IOException e) {
			logger.error("IO Exception in UserInfoJwtView", e);
		} catch (ParseException e) {
			e.printStackTrace();
		}

	}

    private Set<String> getServiceIds(Set<String> scopes) {
    	if (scopes != null && !scopes.isEmpty()) {
    		return serviceManager.findServiceIdsByScopes(scopes);
    	}
    	return Collections.emptySet();
    }

}