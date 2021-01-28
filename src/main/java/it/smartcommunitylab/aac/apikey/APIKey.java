/**
 *    Copyright 2015-2019 Smart Community Lab, FBK
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
 */

package it.smartcommunitylab.aac.apikey;

import java.util.Collections;
import java.util.Map;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.apikey.model.APIKeyEntity;

/**
 * @author raman
 *
 */
@JsonInclude(Include.NON_EMPTY)
public class APIKey {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private String apiKey;
    private String clientId;
    
    @JsonInclude(Include.ALWAYS)
    private String[] scope;

    private String subject;

    private String issuer;
    private Integer expirationTime;
    private Integer issuedAt;

    private Map<String, Object> additionalInformation;
    private Map<String, Object> userClaims;

    private Integer validity;

    public APIKey() {
        this.additionalInformation = Collections.emptyMap();
        this.userClaims = Collections.emptyMap();
    }

    public APIKey(APIKey apikey) {
        this.apiKey = apikey.getApiKey();
        this.clientId = apikey.getClientId();
        this.scope = apikey.getScope();

        this.subject = apikey.getSubject();

        this.issuer = apikey.getIssuer();
        this.expirationTime = apikey.getExpirationTime();
        this.issuedAt = apikey.getIssuedAt();

        this.additionalInformation = apikey.getAdditionalInformation();
        this.userClaims = apikey.getUserClaims();
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String[] getScope() {
        return scope;
    }

    public void setScope(String[] scope) {
        this.scope = scope;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public Integer getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(Integer expirationTime) {
        this.expirationTime = expirationTime;
    }

    public Integer getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Integer issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Map<String, Object> getAdditionalInformation() {
        return additionalInformation;
    }

    public void setAdditionalInformation(Map<String, Object> additionalInformation) {
        this.additionalInformation = additionalInformation;
    }

    public Map<String, Object> getUserClaims() {
        return userClaims;
    }

    public void setUserClaims(Map<String, Object> userClaims) {
        this.userClaims = userClaims;
    }

    public Integer getValidity() {
        return validity;
    }

    public void setValidity(Integer validity) {
        this.validity = validity;
    }

    /*
     * Builder
     */
    @SuppressWarnings("unchecked")
    public static APIKey fromApiKey(APIKeyEntity entity) {
        APIKey dto = new APIKey();
        dto.apiKey = entity.getApiKey();
        dto.clientId = entity.getClientId();
        if (entity.getScope() != null) {
            dto.scope = StringUtils.commaDelimitedListToStringArray(entity.getScope());
        }
        dto.subject = Long.toString(entity.getUserId());

        // issued time is milliseconds
        dto.issuedAt = (int) (entity.getIssuedTime() / 1000);
        if (entity.getValidity() != null) {
            // validity is in seconds
            dto.expirationTime = dto.issuedAt + (int) (entity.getValidity().longValue());
        } else {
            // default to 12 hour
            dto.expirationTime = 0;
        }

        if (entity.getAdditionalInformation() != null) {
            try {
                dto.additionalInformation = objectMapper.readValue(entity.getAdditionalInformation(), Map.class);
            } catch (Exception e) {
                dto.additionalInformation = Collections.emptyMap();
            }
        }

        return dto;
    }

//  @SuppressWarnings("unchecked")
//  public APIKey(APIKeyEntity entity) {
//      super();
//      this.apiKey = entity.getApiKey();
//      this.clientId = entity.getClientId();
//      if (entity.getAdditionalInformation() != null) {
//          try {
//              this.additionalInformation = objectMapper.readValue(entity.getAdditionalInformation(), Map.class);
//          } catch (Exception e) {
//              this.additionalInformation = Collections.emptyMap();
//          }
//      }
//      this.userId = entity.getUserId();
//      this.username = entity.getUsername();
//      this.validity = entity.getValidity();
//      this.issuedTime = entity.getIssuedTime();
//      if (entity.getScope() != null) {
//          this.scope = StringUtils.commaDelimitedListToSet(entity.getScope()); 
//      }
//      if (entity.getRoles() != null) {
//          CollectionType javaType = objectMapper.getTypeFactory()
//                    .constructCollectionType(List.class, Role.class);
//          try {
//              this.roles = new HashSet<>(objectMapper.readValue(entity.getRoles(), javaType));
//          } catch (Exception e) {
//              this.roles = Collections.emptySet();
//          } 
//      }
//  }

    /**
     * @param data
     * @return
     */
    public static String toDataString(Map<String, Object> data) {
        if (data == null)
            return null;
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public static Map<String, Object> toMap(APIKey apikey) {
        Map<String, Object> json = objectMapper.convertValue(apikey, new TypeReference<Map<String, Object>>() {
        });

        // move user claims to top level
        json.remove("userClaims");
        if (apikey.getUserClaims() != null) {
            json.putAll(apikey.getUserClaims());
        }
        return json;
    }

//
//    /**
//     * @return
//     */
//    public boolean hasExpired() {
//        if (getValidity() != null && getValidity() > 0) {
//            return getIssuedTime() + getValidity() < System.currentTimeMillis();
//        }
//        return false;
//    }

}
