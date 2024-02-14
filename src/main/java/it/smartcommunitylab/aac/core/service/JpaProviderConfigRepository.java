/**
 * Copyright 2023 Fondazione Bruno Kessler
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

package it.smartcommunitylab.aac.core.service;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;
import it.smartcommunitylab.aac.base.provider.config.AbstractProviderConfig;
import it.smartcommunitylab.aac.core.persistence.ProviderConfigEntity;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class JpaProviderConfigRepository<U extends AbstractProviderConfig<?, ?>>
    implements ProviderConfigRepository<U> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ObjectMapper mapper;
    private final String type;

    private final ProviderConfigEntityService entityService;
    private final EntityConverter converter;

    public JpaProviderConfigRepository(ProviderConfigEntityService entityService, Class<U> className) {
        Assert.notNull(className, "please provide a valid class for serializer");
        Assert.notNull(entityService, "entity service required");

        this.type = className.getName();
        logger.debug("create jpa repository for provider config {}", type);

        this.entityService = entityService;
        this.mapper = new CBORMapper();
        mapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);

        this.converter = new EntityConverter(mapper, className);
    }

    @Override
    public U findByProviderId(String providerId) {
        ProviderConfigEntity e = entityService.findOne(type, providerId);
        if (e == null) {
            return null;
        }

        return converter.from(e);
    }

    @Override
    public Collection<U> findAll() {
        return entityService.findAll(type).stream().map(converter::from).collect(Collectors.toList());
    }

    @Override
    public Collection<U> findByRealm(String realm) {
        return entityService.findByRealm(type, realm).stream().map(converter::from).collect(Collectors.toList());
    }

    @Override
    public void addRegistration(U registration) {
        if (registration != null) {
            String providerId = registration.getProvider();
            if (StringUtils.hasText(providerId)) {
                logger.debug("add registration for provider {} with id {}", type, providerId);
                try {
                    //always add or update with override
                    ProviderConfigEntity e = new ProviderConfigEntity();
                    e.setProviderId(providerId);
                    e.setType(type);
                    e.setRealm(registration.getRealm());

                    byte[] bytes = mapper.writeValueAsBytes(registration);
                    e.setVersion(registration.getVersion());
                    e.setConfig(bytes);

                    e = entityService.save(type, providerId, e);

                    if (logger.isTraceEnabled()) {
                        logger.trace(
                            "saved registration for provider {} with id {}: {}",
                            type,
                            providerId,
                            String.valueOf(e)
                        );
                    }
                } catch (JsonProcessingException e) {
                    logger.error("error converting registration {} provider {}: {}", type, providerId, e.getMessage());
                    throw new RuntimeException(e.getMessage());
                }
            }
        }
    }

    @Override
    public void removeRegistration(String providerId) {
        entityService.remove(type, providerId);
    }

    @Override
    public void removeRegistration(U registration) {
        if (registration != null) {
            String providerId = registration.getProvider();
            if (StringUtils.hasText(providerId)) {
                removeRegistration(providerId);
            }
        }
    }

    private class EntityConverter {

        private final ObjectMapper mapper;
        private final Class<U> type;

        public EntityConverter(ObjectMapper mapper, Class<U> type) {
            Assert.notNull(mapper, "mapper required");
            this.mapper = mapper;
            this.type = type;
        }

        public U from(ProviderConfigEntity entity) {
            byte[] bytes = entity.getConfig();
            if (bytes == null || bytes.length == 0) {
                return null;
            }

            try {
                U u = mapper.readValue(bytes, type);
                return u;
            } catch (IOException e) {
                logger.error("error reading config for {}: {}", type, e.getMessage());
                throw new RuntimeException(e.getMessage());
            }
        }
    }
}
