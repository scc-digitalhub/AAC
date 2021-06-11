package it.smartcommunitylab.aac.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import it.smartcommunitylab.aac.core.auth.UserAuthentication;

public class ExtendedAuthenticationEventPublisher
        implements AuthenticationEventPublisher, ApplicationEventPublisherAware {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ApplicationEventPublisher applicationEventPublisher;
    private final DefaultAuthenticationEventPublisher defaultPublisher;

    public ExtendedAuthenticationEventPublisher() {
        this(null);
    }

    public ExtendedAuthenticationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
        // build default to handle exceptions
        defaultPublisher = new DefaultAuthenticationEventPublisher(applicationEventPublisher);

    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
        defaultPublisher.setApplicationEventPublisher(applicationEventPublisher);
    }

    public void publishUserAuthenticationSuccess(
            String authority, String provider, String realm,
            UserAuthentication authentication) {
        if (this.applicationEventPublisher != null) {
            UserAuthenticationSuccessEvent event = new UserAuthenticationSuccessEvent(authority, provider, realm,
                    authentication);
            this.applicationEventPublisher.publishEvent(event);
        }
    }

    @Override
    public void publishAuthenticationSuccess(Authentication authentication) {
        // generic event, let default handle
        defaultPublisher.publishAuthenticationSuccess(authentication);
    }

    @Override
    public void publishAuthenticationFailure(AuthenticationException exception, Authentication authentication) {
        // generic event, let default handle
        // TODO build a custom model to carry realm/provider etc
        defaultPublisher.publishAuthenticationFailure(exception, authentication);

    }
}
