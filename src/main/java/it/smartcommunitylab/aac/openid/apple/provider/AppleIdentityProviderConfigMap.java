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

package it.smartcommunitylab.aac.openid.apple.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.model.AbstractConfigMap;
import java.io.Serializable;
import java.util.Map;
import javax.validation.Valid;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppleIdentityProviderConfigMap extends AbstractConfigMap implements Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_APPLE_SERIAL_VERSION;

    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_CONFIG +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.RESOURCE_IDENTITY_PROVIDER +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.AUTHORITY_APPLE;

    private String clientId;
    private String teamId;

    private String keyId;
    private String privateKey;

    private Boolean askNameScope;
    private Boolean askEmailScope;

    private Boolean trustEmailAddress;

    public AppleIdentityProviderConfigMap() {}

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public Boolean getAskNameScope() {
        return askNameScope;
    }

    public void setAskNameScope(Boolean askNameScope) {
        this.askNameScope = askNameScope;
    }

    public Boolean getAskEmailScope() {
        return askEmailScope;
    }

    public void setAskEmailScope(Boolean askEmailScope) {
        this.askEmailScope = askEmailScope;
    }

    public Boolean getTrustEmailAddress() {
        return trustEmailAddress;
    }

    public void setTrustEmailAddress(Boolean trustEmailAddress) {
        this.trustEmailAddress = trustEmailAddress;
    }

    @JsonIgnore
    public void setConfiguration(AppleIdentityProviderConfigMap map) {
        this.clientId = map.getClientId();
        this.teamId = map.getTeamId();
        this.privateKey = map.getPrivateKey();
        this.keyId = map.getKeyId();

        this.askEmailScope = map.getAskEmailScope();
        this.askNameScope = map.getAskNameScope();
        this.trustEmailAddress = map.getTrustEmailAddress();
    }

    @Override
    @JsonIgnore
    public void setConfiguration(Map<String, Serializable> props) {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        AppleIdentityProviderConfigMap map = mapper.convertValue(props, AppleIdentityProviderConfigMap.class);

        setConfiguration(map);
    }

    @Override
    @JsonIgnore
    public JsonSchema getSchema() throws JsonMappingException {
        return schemaGen.generateSchema(AppleIdentityProviderConfigMap.class);
    }
}
