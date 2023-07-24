package it.smartcommunitylab.aac.oauth.event;

import it.smartcommunitylab.aac.SystemKeys;
import java.io.Serializable;
import org.springframework.context.ApplicationEvent;

public abstract class OAuth2Event extends ApplicationEvent {

    private static final long serialVersionUID = SystemKeys.AAC_OAUTH2_SERIAL_VERSION;

    public OAuth2Event(Serializable request) {
        super(request);
    }
}
