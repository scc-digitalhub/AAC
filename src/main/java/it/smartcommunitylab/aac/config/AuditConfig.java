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

package it.smartcommunitylab.aac.config;

import it.smartcommunitylab.aac.audit.AuthenticationEventListener;
import it.smartcommunitylab.aac.audit.AuthorizationEventListener;
import it.smartcommunitylab.aac.audit.ExtendedAuthenticationEventPublisher;
import it.smartcommunitylab.aac.audit.store.AutoJdbcAuditEventStore;
import it.smartcommunitylab.aac.identity.service.IdentityProviderService;
import it.smartcommunitylab.aac.oauth.event.OAuth2EventPublisher;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

/*
 * Audit configuration
 * Event based trail with persistence
 */

@Configuration
@Order(5)
public class AuditConfig {

    @Autowired
    private DataSource dataSource;

    @Bean(name = "applicationEventMulticaster")
    public ApplicationEventMulticaster simpleApplicationEventMulticaster() {
        SimpleApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();

        eventMulticaster.setTaskExecutor(new SimpleAsyncTaskExecutor());
        return eventMulticaster;
    }

    @Bean
    public ExtendedAuthenticationEventPublisher authenticationEventPublisher(
        ApplicationEventPublisher applicationEventPublisher,
        IdentityProviderService providerService
    ) {
        ExtendedAuthenticationEventPublisher publisher = new ExtendedAuthenticationEventPublisher(
            applicationEventPublisher
        );
        publisher.setProviderService(providerService);

        return publisher;
    }

    @Bean
    public OAuth2EventPublisher oauth2EventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        return new OAuth2EventPublisher(applicationEventPublisher);
    }

    //    @Bean
    //    public AuditEventRepository auditEventRepository() {
    //        return new InMemoryAuditEventRepository();
    //    }

    @Bean
    public AutoJdbcAuditEventStore auditEventRepository() {
        return new AutoJdbcAuditEventStore(dataSource);
    }

    @Bean
    public AuthenticationEventListener authenticationEventListener(IdentityProviderService providerService) {
        AuthenticationEventListener listener = new AuthenticationEventListener();
        listener.setProviderService(providerService);

        return listener;
    }

    @Bean
    public AuthorizationEventListener authorizationEventListener() {
        return new AuthorizationEventListener();
    }
}
