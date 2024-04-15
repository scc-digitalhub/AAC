/*
 * Copyright 2023 the original author or authors
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

package it.smartcommunitylab.aac.openid.session;

import it.smartcommunitylab.aac.common.SystemException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

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
