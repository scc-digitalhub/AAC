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

package it.smartcommunitylab.aac.services;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.smartcommunitylab.aac.claims.model.AbstractClaim;
import it.smartcommunitylab.aac.model.AttributeType;
import it.smartcommunitylab.aac.services.persistence.ServiceClaimEntity;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Valid
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceClaim extends AbstractClaim {

    private static ObjectMapper mapper = new ObjectMapper();

    @Size(max = 128)
    private String serviceId;

    @NotNull
    private AttributeType type;

    private boolean multiple = false;

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public AttributeType getType() {
        return type;
    }

    public void setType(AttributeType type) {
        this.type = type;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    /*
     * Builder
     */

    public static ServiceClaim from(ServiceClaimEntity entity) {
        ServiceClaim claim = new ServiceClaim();
        claim.key = entity.getKey();
        claim.serviceId = entity.getServiceId();
        claim.name = entity.getName();
        claim.description = entity.getDescription();
        claim.type = entity.getType() != null ? AttributeType.parse(entity.getType()) : AttributeType.OBJECT;
        claim.multiple = entity.isMultiple();

        return claim;
    }

    /**
     * Fully qualified claim name (namespace / local claim)
     *
     * @param namespace
     * @param claim
     * @return
     */
    public static String qualifiedName(String namespace, String claim) {
        return namespace == null ? claim : String.format("%s/%s", namespace, claim);
    }

    /**
     * Check claim type
     *
     * @param value
     * @param multiple
     * @param type
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static boolean ofType(Object value, boolean multiple, AttributeType type) {
        if (value == null) return true;
        boolean vMultiple = (value.getClass().isArray() || value instanceof Collection);
        if (vMultiple != multiple) {
            return false;
        }
        Object v = value.getClass().isArray()
            ? ((Object[]) value).length > 0 ? ((Object[]) value)[0] : null
            : (value instanceof Collection)
                ? ((Collection) value).size() > 0 ? ((Collection) value).iterator().next() : null
                : value;

        if (v == null) return true;

        if (type.equals(AttributeType.NUMBER)) {
            try {
                Double.parseDouble(v.toString());
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        if (type.equals(AttributeType.BOOLEAN)) {
            try {
                Boolean.parseBoolean(v.toString());
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        if (
            (v instanceof String && type.equals(AttributeType.STRING)) ||
            (v instanceof Map && type.equals(AttributeType.OBJECT))
        ) {
            return true;
        }

        return false;
    }

    /**
     * Return typed value of the claim parsing the string representation
     *
     * @param claim
     * @param value
     * @return
     */
    public static Object typedValue(ServiceClaim claim, String value) {
        try {
            if (claim.isMultiple()) {
                return mapper.readValue(value, LinkedList.class);
            }

            switch (claim.getType()) {
                case BOOLEAN:
                    return Boolean.parseBoolean(value);
                case NUMBER: {
                    try {
                        return Integer.parseInt(value);
                    } catch (Exception e) {
                        return Double.parseDouble(value);
                    }
                }
                case STRING:
                    return mapper.readValue(value, String.class);
                case OBJECT:
                    return mapper.readValue(value, HashMap.class);
                default:
                    return value;
            }
        } catch (Exception e) {
            return value;
        }
    }

    @Override
    public Serializable getValue() {
        // nothing to return on definition
        return null;
    }
}
