package it.smartcommunitylab.aac.services.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "service_clients", uniqueConstraints = @UniqueConstraint(columnNames = { "service_id", "client_id" }))
public class ServiceClientEntity {

    @Id
    @GeneratedValue
    @Column(name = "claim_id")
    private Long id;

    @NotNull
    @Column(name = "service_id")
    private String serviceId;

    @NotNull
    @Column(name = "client_id")
    private String clientId;

    @Column(name = "client_type")
    private String type;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

}
