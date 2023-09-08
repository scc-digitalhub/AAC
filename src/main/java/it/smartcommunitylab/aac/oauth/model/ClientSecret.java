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

package it.smartcommunitylab.aac.oauth.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.clients.model.ClientCredentials;
import it.smartcommunitylab.aac.oauth.persistence.AbstractOAuth2ClientResource;
import jakarta.validation.Valid;
import org.springframework.util.Assert;

@Valid
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientSecret extends AbstractOAuth2ClientResource implements ClientCredentials {

    private static final long serialVersionUID = SystemKeys.AAC_OAUTH2_SERIAL_VERSION;

    private String secret;

    public ClientSecret(String realm, String clientId, String secret) {
        super(realm, clientId);
        Assert.notNull(secret, "secret can not be null");
        this.secret = secret;
    }

    @Override
    public final String getType() {
        return SystemKeys.RESOURCE_CREDENTIALS_SECRET;
    }

    @Override
    @JsonIgnore
    public String getCredentials() {
        return secret;
    }

    public String getClientSecret() {
        return secret;
    }

    @Override
    public String getId() {
        return getClientId() + "." + getType();
    }

    @Override
    public void eraseCredentials() {
        this.secret = null;
    }
}
