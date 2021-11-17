package it.smartcommunitylab.aac.openid.session;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import it.smartcommunitylab.aac.common.SystemException;

public class Sha256SessionStateGenerator implements SessionStateGenerator {

    @Override
    public String generateState(String clientId, String originUrl, String userAgentState, String salt) {
        try {
            // build a string to feed encoder
            StringBuilder sb = new StringBuilder();
            sb.append(clientId).append(originUrl);
            sb.append(salt);
            sb.append(userAgentState);
            String value = sb.toString();

            // build hash
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            byte[] digest = md.digest(value.getBytes(StandardCharsets.US_ASCII));
            String hash = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);

            // return hash+salt
            return hash.concat(".").concat(salt);
        } catch (NoSuchAlgorithmException e) {
            throw new SystemException();
        }
    }

}
