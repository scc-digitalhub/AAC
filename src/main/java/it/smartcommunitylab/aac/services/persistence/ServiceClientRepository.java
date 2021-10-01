package it.smartcommunitylab.aac.services.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface ServiceClientRepository extends CustomJpaRepository<ServiceClientEntity, Long> {

    List<ServiceClientEntity> findByServiceId(String serviceId);

    List<ServiceClientEntity> findByServiceIdAndType(String serviceId, String type);

    ServiceClientEntity findByServiceIdAndClientId(String serviceId, String clientId);

}
