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

package it.smartcommunitylab.aac.manager;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;

import javax.crypto.Cipher;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.model.UserCertificate;
import it.smartcommunitylab.aac.oauth.AACOAuthRequest;
import it.smartcommunitylab.aac.repository.UserCertificateRepository;

/**
 * @author raman
 *
 */
@Component
@Transactional
public class CIEAuthManager implements MobileAuthManager {

	private static final Logger log = LoggerFactory.getLogger(CIEAuthManager.class);
	
	private static final String CIE_PROVIDER = "cie";
	
	@Autowired
	private UserCertificateRepository repo;
	@Autowired
	private UserManager userManager;
	
	@Value("${security.cie.idp}")
	private String idp;
	@Value("${security.cie.pattern}")
	private String pattern;
	@Value("${security.cie.defaultsp}")
	private String defaultSp;
	
	@Autowired
	private IdentitySource identitySource;
	@Autowired
	private BasicProfileManager profileManager;
	
	@Override
	public String provider() {
		return CIE_PROVIDER;
	}

	/* (non-Javadoc)
	 * @see it.smartcommunitylab.aac.manager.MobileAuthManager#makeCertCheck(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public String init2FactorCheck(HttpServletRequest request, String redirect) throws SecurityException {
	    long challenge = (long) (Math.random() * 1_0000_0000_0000_0000L);
	    try {
	        boolean isValidCert = isValidCertForUser();
            String url = compileURL(challenge, !isValidCert, request, getSP(request), redirect);
            return url;
	    } catch (CertificateException e) {
	        log.error(e.getMessage(), e);
            throw new SecurityException(e.getMessage());
	    }
	}

	/**
	 * Take SP name from the stored OAuth2 client or default AAC value
	 * @param request
	 * @return
	 */
	private String getSP(HttpServletRequest request) {
		AACOAuthRequest oauthRequest = (AACOAuthRequest) request.getSession().getAttribute(Config.SESSION_ATTR_AAC_OAUTH_REQUEST);
		if (oauthRequest != null) {
			return oauthRequest.getClientApp();
		}
		return defaultSp;
	}

	/* (non-Javadoc)
	 * @see it.smartcommunitylab.aac.manager.MobileAuthManager#checkSignature(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public void callback2FactorCheck(final HttpServletRequest request) throws SecurityException {
	    String signature = request.getParameter("signature");
	    log.info("signature: "+signature);
	    String cert = request.getParameter("cert");
	    log.info("cert: "+cert);
	    String r1 = request.getParameter("r1");
	    log.info("r1: "+r1);
	    Long opId = (Long) request.getSession().getAttribute("opId");
	    log.info("opId: " + opId);
	    String idpName = (String)  request.getSession().getAttribute("idpName");
	    log.info("idpName: " + idpName);
	    String spName = (String)  request.getSession().getAttribute("spName");
	    log.info("spName: " + spName);
	    Long timestamp = (Long)  request.getSession().getAttribute("timestamp");
	    log.info("timestamp: " + timestamp);
	    String textOp = (String)  request.getSession().getAttribute("textOp");
	    log.info("textOp: " + textOp);
	    String knownSource = opId
	            + ", " + timestamp
	            + ", " + spName
	            + ", " + idpName
	            + ", " + textOp;
	
	    log.info("knownSource: " + knownSource);
	    try {
	        X509Certificate x509Certificate = convertToX509Cert(cert);
	        if (!isValidX509Cert(x509Certificate)) {
                throw new SecurityException("Invalid certificate");
	        }
	        String username = getUsername();
	        if (signature != null) {
            	UserCertificate certRecord = null;
                if(cert != null && !cert.isEmpty() && isValidX509Cert(convertToX509Cert(cert))) {
                	certRecord = repo.findByUsernameAndProvider(username, CIE_PROVIDER);
                	// assume pairing has already been done
                	if (certRecord != null) {
                		certRecord.setCertificate(cert);
                	}
                }
                if (cert != null) {
                    boolean certIsOk = isOkSigned(knownSource, r1, signature, convertToX509Cert(cert));
                    boolean identitiesMatch = identitiesMatch(x509Certificate, username);
                    if (certIsOk && identitiesMatch) {
                        if ( request.getSession().getAttribute("invalidSignature") != null) {
                        	 request.getSession().removeAttribute("invalidSignature");
                        }
    	                if (certRecord != null) {
    	            		repo.save(certRecord);
    	                }
    	                return;
                    } else {
                	 request.getSession().setAttribute("invalidSignature", "Mobile app signature is invalid or identities do not match");
                	 throw new SecurityException("Mobile app signature is invalid or identities do not match");
                    }
                }
	        }
	    } catch (CertificateException e) {
	        log.error(e.getMessage(), e);
	        throw new SecurityException(e.getMessage());
	    }
	}

	/**
	 * @return username from the security context.
	 */
	private String getUsername() {
		return userManager.getUserId().toString();
	}

	/**
	 * Check if the certificate exists and is valid
	 * @return
	 * @throws CertificateException
	 */
	private boolean isValidCertForUser() throws CertificateException {
        String cert = extractCertificate(getUsername());
        return (cert != null && !cert.isEmpty()) && isValidX509Cert(convertToX509Cert(cert));
    }

	/**
	 * Generate a redirect URL for the mobile CIE application.
	 * @param opId challenge
	 * @param certRequest whether certificate string should be requested  
	 * @param redirect 
	 * @param requestContext
	 * @return
	 */
    private String compileURL(long opId, boolean certRequest, HttpServletRequest request, String spName, String redirect) {
        String idpName = idp;
        String textOp = String.format(pattern, spName, idpName);
        String pattern = "https://idp-ipzs.fbk.eu/CustomTab?" +
                "OpId=%1$d" +
                "&Time=%2$d" +
                "&SP=%3$s" +
                "&IdP=%4$s" +
                "&Text_Op=" + textOp +
                "&certRequest=%5$b" +
                "&nextURL=%6$s?execution=e2s1";
        Long timestamp = System.currentTimeMillis();
        request.getSession().setAttribute("opId",opId);
        request.getSession().setAttribute("idpName",idpName);
        request.getSession().setAttribute("spName",spName);
        request.getSession().setAttribute("timestamp",timestamp);
        request.getSession().setAttribute("textOp",textOp);
        String url = String.format(pattern, opId, timestamp, spName, idpName, certRequest, redirect);
        return url;
    }

    /**
     * Check signature correctness
     * @param knownSource
     * @param randomNumber
     * @param signature
     * @param cert
     * @return
     */
    private boolean isOkSigned(String knownSource, String randomNumber, String signature, X509Certificate cert) {
        try {
            RSAPublicKey publicKey = (RSAPublicKey) cert.getPublicKey();
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding ");
            cipher.init(Cipher.DECRYPT_MODE,publicKey);
            // String decryptedClientSource = Arrays.toString(cipher.doFinal(arraysToStringBackToArray(hashedFromClient)));
            byte[] decryptedBytes = cipher.doFinal(hexStringToByteArray(signature));
            String decryptedHash = bytesToHex(decryptedBytes);

            return isHashesEquals(decryptedHash, knownSource, randomNumber);
        }catch (Exception ex){
            log.error(ex.getMessage(),ex);
            return false;
        }
    }

    /**
     * Check that the stored user identity and the one provided by the certificate match 
     * @param certificate
     * @param userName
     * @return
     */
    private boolean identitiesMatch(X509Certificate certificate, String userName) {
        String codeFromCertificate = parseCN(certificate.getSubjectX500Principal().getName());
        log.info("identityFromCertificate: " + codeFromCertificate);
        String codeFromDB = getStoredCN(userName);
        log.info("codeFromDB: " + codeFromDB);
        return codeFromCertificate.trim().toLowerCase().equals(codeFromDB.trim().toLowerCase());
    }

    /**
     * Extrqact user identity from the principal data
     * @param principal
     * @return
     */
    private String parseCN(String principal) {
        String cn = null;
        for (String value : principal.split(",")) {
            if (value.trim().startsWith("CN")) {
                return value.split("=")[1].split("/")[0];
            }
        }return cn;
    }


    private String extractCertificate(final String username) {
        UserCertificate cert = repo.findByUsernameAndProvider(username, CIE_PROVIDER);
        return cert != null ? cert.getCertificate() : null;
    }

    private String getStoredCN(String username) {
    	return identitySource.getUserIdentity(CIE_PROVIDER, Long.parseLong(username), profileManager.getAccountProfileById(username));
    }

    private boolean isValidX509Cert(X509Certificate cert) {
        if (cert == null) return false;
        else {
            try {
                cert.checkValidity();
                return true;
            } catch (CertificateExpiredException | CertificateNotYetValidException e) {
                return false;
            }
        }
    }

    private X509Certificate convertToX509Cert(String certificateString) throws CertificateException {
        X509Certificate certificate = null;
        CertificateFactory cf = null;
        try {
            if (certificateString != null && !certificateString.trim().isEmpty()) {
                byte[] certificateData = hexStringToByteArray(certificateString);
                cf = CertificateFactory.getInstance("X509");
                certificate = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certificateData));
            }
        } catch (CertificateException e) {
            log.error(e.getMessage(), e);
            throw new CertificateException(e);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return certificate;
    }

    private boolean isHashesEquals(String hashed, String knownSource, String randomNumber) throws Exception {
      log.info("is hash equal: " + hashed.equals(createHash(knownSource, randomNumber)));
      return hashed.equals(createHash(knownSource, randomNumber));
    }

    private String createHash(String knownSources, String randomNumber) throws Exception {
      byte[] nonceByte = hexStringToByteArray(randomNumber);

        byte[] arr = appendByteArray(nonceByte, knownSources.getBytes());
        MessageDigest md = MessageDigest.getInstance("SHA-256");

        return bytesToHex(md.digest(arr));
    }

    private byte[] hexStringToByteArray(String s) throws Exception{
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private String bytesToHex (byte[] bytes) throws Exception {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (int i=0; i< bytes.length; i++) {
            sb.append(String.format("%02x", bytes[i]));
        }
        return sb.toString();
    }

    private byte[] appendByteArray(byte[] randomNumberBytes, byte[] knownSourcesBytes) throws Exception {
        byte[] resultArray = new byte[randomNumberBytes.length + knownSourcesBytes.length];
        System.arraycopy(randomNumberBytes, 0, resultArray, 0, randomNumberBytes.length);
        System.arraycopy(knownSourcesBytes, 0, resultArray, randomNumberBytes.length, knownSourcesBytes.length);
        return resultArray;
    }
}
