package it.smartcommunitylab.aac.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.security.AbstractAuthorizationAuditListener;
import org.springframework.security.access.event.AbstractAuthorizationEvent;

public class AuthorizationEventListener extends AbstractAuthorizationAuditListener {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String AUTHORIZATION_FAILURE = "AUTHORIZATION_FAILURE";

    @Override
    public void onApplicationEvent(AbstractAuthorizationEvent event) {
        // just log, we don't persist these for now
        // TODO handle without persisting all anonymous authFailure on no auth
        // each request without authentication triggers an unauthorized event which ends
        // in store, we don't want those in db
        logger.trace("authorization event " + event.toString());
    }

}
