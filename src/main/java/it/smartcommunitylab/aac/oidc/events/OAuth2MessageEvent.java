/**
 * Copyright 2024 the original author or authors
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

package it.smartcommunitylab.aac.oidc.events;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import it.smartcommunitylab.aac.events.ProviderEmittedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.util.Assert;

public abstract class OAuth2MessageEvent extends ApplicationEvent implements ProviderEmittedEvent {

    private final String authority;
    private final String provider;
    private final String realm;

    private String tx;

    protected OAuth2MessageEvent(String authority, String provider, String realm, Object source) {
        super(source);
        Assert.hasText(provider, "provider identifier can not be null or blank");
        this.authority = authority;
        this.provider = provider;
        this.realm = realm;
    }

    public String getAuthority() {
        return authority;
    }

    public String getProvider() {
        return provider;
    }

    public String getRealm() {
        return realm;
    }

    public void setTx(String tx) {
        this.tx = tx;
    }

    @JsonInclude(Include.NON_NULL)
    public String getTx() {
        return tx;
    }

    @JsonIgnore
    @Override
    public Object getSource() {
        return super.getSource();
    }
}
