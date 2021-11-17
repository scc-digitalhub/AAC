package it.smartcommunitylab.aac.services;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import it.smartcommunitylab.aac.services.persistence.ServiceClientEntity;

@Valid
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceClient {
    @NotBlank
    private String serviceId;

    private String clientId;

    @NotBlank
    private String type;

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public static ServiceClient from(ServiceClientEntity entity) {
        ServiceClient client = new ServiceClient();
        client.serviceId = entity.getServiceId();
        client.clientId = entity.getClientId();
        client.type = entity.getType();

        return client;
    }

}
