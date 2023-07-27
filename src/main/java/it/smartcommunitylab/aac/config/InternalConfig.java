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

import it.smartcommunitylab.aac.webauthn.store.InMemoryWebAuthnAssertionRequestStore;
import it.smartcommunitylab.aac.webauthn.store.InMemoryWebAuthnRegistrationRequestStore;
import it.smartcommunitylab.aac.webauthn.store.WebAuthnAssertionRequestStore;
import it.smartcommunitylab.aac.webauthn.store.WebAuthnRegistrationRequestStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.annotation.Order;
import org.springframework.web.context.WebApplicationContext;

@Configuration
@Order(6)
public class InternalConfig {

    @Bean
    @Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
    public WebAuthnAssertionRequestStore webAuthnAssertionRequestStore() {
        // used as session scoped proxy, we need this for in flight requests
        return new InMemoryWebAuthnAssertionRequestStore();
    }

    @Bean
    @Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
    public WebAuthnRegistrationRequestStore webAuthnRegistrationRequestStore() {
        // used as session scoped proxy, we need this for in flight requests
        return new InMemoryWebAuthnRegistrationRequestStore();
    }
}
