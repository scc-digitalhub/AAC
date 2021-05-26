package it.smartcommunitylab.aac.oauth.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.TokenRequest;

public class OAuth2EventPublisher implements ApplicationEventPublisherAware {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ApplicationEventPublisher applicationEventPublisher;

    public OAuth2EventPublisher() {
        this(null);
    }

    public OAuth2EventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publishTokenGrant(OAuth2AccessToken token, OAuth2Authentication authentication) {
        if (this.applicationEventPublisher != null) {
            this.applicationEventPublisher.publishEvent(new TokenGrantEvent(token, authentication));
        }
    }

    public void publishOAuth2AuthorizationException(AuthorizationRequest request, OAuth2Exception exception,
            OAuth2Authentication authentication) {
        if (this.applicationEventPublisher != null) {
            this.applicationEventPublisher
                    .publishEvent(new OAuth2AuthorizationExceptionEvent(request, exception, authentication));
        }
    }

    public void publishOAuth2TokenException(TokenRequest request, OAuth2Exception exception,
            OAuth2Authentication authentication) {
        if (this.applicationEventPublisher != null) {
            this.applicationEventPublisher
                    .publishEvent(new OAuth2TokenExceptionEvent(request, exception, authentication));
        }
    }
}
