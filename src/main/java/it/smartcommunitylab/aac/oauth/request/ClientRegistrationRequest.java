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

package it.smartcommunitylab.aac.oauth.request;

import com.nimbusds.jwt.SignedJWT;
import it.smartcommunitylab.aac.oauth.model.ClientRegistration;
import org.springframework.util.Assert;

public class ClientRegistrationRequest {

    /*
     * Client metadata
     */
    private final ClientRegistration registration;

    /*
     * Optional software statement as JWT
     */
    private final SignedJWT softwareStatement;

    public ClientRegistrationRequest(ClientRegistration registration) {
        this(registration, null);
    }

    public ClientRegistrationRequest(ClientRegistration registration, SignedJWT softwareStatement) {
        Assert.notNull(registration, "client registration can not be null");
        this.registration = registration;
        this.softwareStatement = softwareStatement;
    }

    public ClientRegistration getRegistration() {
        return registration;
    }

    public SignedJWT getSoftwareStatement() {
        return softwareStatement;
    }
}
