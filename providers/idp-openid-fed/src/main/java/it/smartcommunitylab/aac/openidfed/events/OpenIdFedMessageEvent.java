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

package it.smartcommunitylab.aac.openidfed.events;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import it.smartcommunitylab.aac.events.ProviderEmittedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.util.Assert;

public abstract class OpenIdFedMessageEvent extends ApplicationEvent implements ProviderEmittedEvent {

    private final String authority;
    private final String provider;
    private final String realm;

    private String trustAnchor;
    private String entityId;

    protected OpenIdFedMessageEvent(String authority, String provider, String realm, Object source) {
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

    @JsonInclude(Include.NON_NULL)
    public String getTrustAnchor() {
        return trustAnchor;
    }

    public void setTrustAnchor(String trustAnchor) {
        this.trustAnchor = trustAnchor;
    }

    @JsonInclude(Include.NON_NULL)
    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    @JsonIgnore
    @Override
    public Object getSource() {
        return super.getSource();
    }
}
