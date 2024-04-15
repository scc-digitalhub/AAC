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

package it.smartcommunitylab.aac.services.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

/**
 * @author raman
 *
 */
@Entity
@Table(name = "service_claim", uniqueConstraints = @UniqueConstraint(columnNames = { "service_id", "claim_key" }))
public class ServiceClaimEntity {

    //    private static ObjectMapper mapper = new ObjectMapper();

    @Id
    @GeneratedValue
    @Column(name = "claim_id", length = 128)
    private Long claimId;

    @Column(name = "claim_key", length = 128)
    private String key;

    /**
     * ServiceDescriptor ID
     */
    //    @ManyToOne(optional = false)
    //    @OnDelete(action = OnDeleteAction.CASCADE)
    //    @JoinColumn(nullable = false, name = "service_id", referencedColumnName = "service_id")
    //    private ServiceEntity service;
    @NotNull
    @Column(name = "service_id", length = 128)
    private String serviceId;

    /**
     * Human-readable claim name
     */
    private String name;
    private String description;

    /**
     * If claim type is multiple
     */
    @Column(name = "claim_multiple")
    private boolean multiple;

    //    @Enumerated(EnumType.STRING)
    //    private AttributeType type;

    @Column(name = "claim_type", length = 32)
    private String type;

    public Long getClaimId() {
        return claimId;
    }

    public void setClaimId(Long claimId) {
        this.claimId = claimId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    //    public ServiceEntity getService() {
    //        return service;
    //    }
    //
    //    public void setService(ServiceEntity service) {
    //        this.service = service;
    //    }
    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    //    /**
    //     * Fully qualified claim name (namespace / local claim)
    //     *
    //     * @param namespace
    //     * @param claim
    //     * @return
    //     */
    //    public static String qualifiedName(String namespace, String claim) {
    //        return namespace == null ? claim : String.format("%s/%s", namespace, claim);
    //    }

    //    /**
    //     * Check claim type
    //     *
    //     * @param value
    //     * @param multiple
    //     * @param type
    //     * @return
    //     */
    //    @SuppressWarnings("rawtypes")
    //    public static boolean ofType(Object value, boolean multiple, AttributeType type) {
    //        if (value == null)
    //            return true;
    //        boolean vMultiple = (value.getClass().isArray() || value instanceof Collection);
    //        if (vMultiple != multiple) {
    //            return false;
    //        }
    //        Object v = value.getClass().isArray()
    //                ? ((Object[]) value).length > 0 ? ((Object[]) value)[0] : null
    //                : (value instanceof Collection)
    //                        ? ((Collection) value).size() > 0 ? ((Collection) value).iterator().next() : null
    //                        : value;
    //
    //        if (v == null)
    //            return true;
    //
    //        if (type.equals(AttributeType.NUMBER)) {
    //            try {
    //                Double.parseDouble(v.toString());
    //                return true;
    //            } catch (NumberFormatException e) {
    //                return false;
    //            }
    //        }
    //        if (type.equals(AttributeType.BOOLEAN)) {
    //            try {
    //                Boolean.parseBoolean(v.toString());
    //                return true;
    //            } catch (NumberFormatException e) {
    //                return false;
    //            }
    //        }
    //
    //        if (v instanceof String && type.equals(AttributeType.STRING) ||
    //                v instanceof Map && type.equals(AttributeType.OBJECT)) {
    //            return true;
    //        }
    //
    //        return false;
    //    }
    //
    //    /**
    //     * Return typed value of the claim parsing the string representation
    //     *
    //     * @param claim
    //     * @param value
    //     * @return
    //     */
    //    public static Object typedValue(ServiceClaimEntity claim, String value) {
    //        try {
    //            if (claim.isMultiple())
    //                return mapper.readValue(value, LinkedList.class);
    //            switch (claim.getType()) {
    //            case BOOLEAN:
    //                return Boolean.parseBoolean(value);
    //            case NUMBER: {
    //                try {
    //                    return Integer.parseInt(value);
    //                } catch (Exception e) {
    //                    return Double.parseDouble(value);
    //                }
    //            }
    //            case STRING:
    //                return mapper.readValue(value, String.class);
    //            case OBJECT:
    //                return mapper.readValue(value, HashMap.class);
    //            default:
    //                return value;
    //            }
    //        } catch (Exception e) {
    //            return value;
    //        }
    //    }

}
