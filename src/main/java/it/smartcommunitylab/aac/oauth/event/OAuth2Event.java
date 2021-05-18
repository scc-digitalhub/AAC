package it.smartcommunitylab.aac.oauth.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

public abstract class OAuth2Event extends ApplicationEvent {

    public OAuth2Event(OAuth2Authentication authentication) {
        super(authentication);
    }

    public OAuth2Authentication getAuthentication() {
        return (OAuth2Authentication) super.getSource();
    }

}
