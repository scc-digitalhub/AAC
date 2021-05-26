package it.smartcommunitylab.aac.oauth.event;

import java.io.Serializable;

import org.springframework.context.ApplicationEvent;

public abstract class OAuth2Event extends ApplicationEvent {

    public OAuth2Event(Serializable request) {
        super(request);
    }

}
