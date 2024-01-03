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

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import it.smartcommunitylab.aac.audit.ExtendedAuthenticationEventPublisher;
import it.smartcommunitylab.aac.audit.listeners.AuthorizationEventListener;
import it.smartcommunitylab.aac.audit.listeners.ClientAuthenticationEventListener;
import it.smartcommunitylab.aac.audit.listeners.UserAuthenticationEventListener;
import it.smartcommunitylab.aac.audit.store.AutoJdbcAuditEventStore;
import it.smartcommunitylab.aac.audit.store.SignedAuditDataReader;
import it.smartcommunitylab.aac.audit.store.SignedAuditDataWriter;
import it.smartcommunitylab.aac.identity.service.IdentityProviderService;
import it.smartcommunitylab.aac.jose.JWKSetKeyStore;
import it.smartcommunitylab.aac.oauth.event.OAuth2EventPublisher;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.util.StringUtils;

/*
 * Audit configuration
 * Event based trail with persistence
 */

@Configuration
@Order(5)
public class AuditConfig {

    @Autowired
    private DataSource dataSource;

    @Value("${audit.issuer}")
    private String issuer;

    @Value("${audit.kid.sig}")
    private String sigKid;

    @Value("${audit.kid.enc}")
    private String encKid;

    @Autowired
    private JWKSetKeyStore jwtKeyStore;

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
        AutoJdbcAuditEventStore store = new AutoJdbcAuditEventStore(dataSource);

        if (StringUtils.hasText(sigKid)) {
            //build signed converters
            JWK jwk = jwtKeyStore
                .getKeys()
                .stream()
                .filter(j ->
                    j.getKeyID().equals(sigKid) && (j.getKeyUse() == null || j.getKeyUse().equals(KeyUse.SIGNATURE))
                )
                .findFirst()
                .orElse(null);

            if (jwk != null) {
                SignedAuditDataWriter writer = new SignedAuditDataWriter(issuer, jwk);
                SignedAuditDataReader reader = new SignedAuditDataReader(issuer, jwtKeyStore.getJwkSet());

                store.setWriter(writer);
                store.setReader(reader);
            }
        }

        return store;
    }

    @Bean
    public UserAuthenticationEventListener userAuthenticationEventListener(IdentityProviderService providerService) {
        UserAuthenticationEventListener listener = new UserAuthenticationEventListener();
        listener.setProviderService(providerService);

        return listener;
    }

    @Bean
    public ClientAuthenticationEventListener clientAuthenticationEventListener() {
        return new ClientAuthenticationEventListener();
    }

    @Bean
    public AuthorizationEventListener authorizationEventListener() {
        return new AuthorizationEventListener();
    }
}
