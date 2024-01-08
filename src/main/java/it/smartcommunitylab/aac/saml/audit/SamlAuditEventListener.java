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

package it.smartcommunitylab.aac.saml.audit;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.audit.store.AuditApplicationEventMixIns;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.saml.events.SamlRequestEvent;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProviderConfig;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SamlAuditEventListener implements ApplicationListener<SamlRequestEvent>, ApplicationEventPublisherAware {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String SAML_REQUEST = "SAML_REQUEST";

    private final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        //include only non-null fields
        .setSerializationInclusion(Include.NON_NULL)
        //add mixin for including typeInfo in events
        .addMixIn(ApplicationEvent.class, AuditApplicationEventMixIns.class);
    private final TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};

    private ApplicationEventPublisher publisher;

    private ProviderConfigRepository<SamlIdentityProviderConfig> registrationRepository;

    @Autowired
    public void setRegistrationRepository(ProviderConfigRepository<SamlIdentityProviderConfig> registrationRepository) {
        this.registrationRepository = registrationRepository;
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void onApplicationEvent(SamlRequestEvent event) {
        String authority = event.getAuthority();
        String provider = event.getProvider();
        String realm = event.getRealm();
        String relayState = event.getRelayState();

        logger.debug("receive saml request event for {}:{} key {}", authority, provider, String.valueOf(relayState));

        if (registrationRepository == null || publisher == null) {
            logger.debug("invalid configuration, skip event");
            return;
        }

        SamlIdentityProviderConfig config = registrationRepository.findByProviderId(provider);
        if (config == null) {
            logger.debug("missing provider configuration, skip event");
            return;
        }

        String level = config.getSettingsMap().getEvents();
        if (SystemKeys.EVENTS_LEVEL_NONE.equals(level)) {
            logger.debug("provider configuration level none, skip event");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("authority", authority);
        data.put("provider", provider);
        data.put("realm", realm);

        if (StringUtils.hasText(relayState)) {
            data.put("tx", relayState);
        }

        if (SystemKeys.EVENTS_LEVEL_FULL.equals(level)) {
            //serialize to avoid exposing object to audit
            data.put("event", mapper.convertValue(event, typeRef));
        }

        AuditApplicationEvent auditEvent = new AuditApplicationEvent(
            Instant.ofEpochMilli(event.getTimestamp()),
            provider,
            SAML_REQUEST,
            data
        );

        logger.debug("publish saml request event for audit");
        if (logger.isTraceEnabled()) {
            logger.trace("audit event: {}", auditEvent);
        }

        publisher.publishEvent(auditEvent);
    }
}
