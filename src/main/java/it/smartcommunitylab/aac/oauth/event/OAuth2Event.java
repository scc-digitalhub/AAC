package it.smartcommunitylab.aac.oauth.event;

import java.io.Serializable;

import org.springframework.context.ApplicationEvent;

import it.smartcommunitylab.aac.SystemKeys;

public abstract class OAuth2Event extends ApplicationEvent {

    private static final long serialVersionUID = SystemKeys.AAC_OAUTH2_SERIAL_VERSION;

    public OAuth2Event(Serializable request) {
        super(request);
    }

}
