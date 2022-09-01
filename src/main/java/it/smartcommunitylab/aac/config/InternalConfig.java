package it.smartcommunitylab.aac.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.annotation.Order;
import org.springframework.web.context.WebApplicationContext;

import it.smartcommunitylab.aac.webauthn.store.InMemoryWebAuthnAssertionRequestStore;
import it.smartcommunitylab.aac.webauthn.store.InMemoryWebAuthnRegistrationRequestStore;
import it.smartcommunitylab.aac.webauthn.store.WebAuthnAssertionRequestStore;
import it.smartcommunitylab.aac.webauthn.store.WebAuthnRegistrationRequestStore;

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
