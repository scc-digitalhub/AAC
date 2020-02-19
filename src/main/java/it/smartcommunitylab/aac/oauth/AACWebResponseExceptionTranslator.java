package it.smartcommunitylab.aac.oauth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.error.DefaultWebResponseExceptionTranslator;

public class AACWebResponseExceptionTranslator extends DefaultWebResponseExceptionTranslator {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public ResponseEntity<OAuth2Exception> translate(Exception e) throws Exception {
        logger.trace("translate exception " + e.getMessage());

        return super.translate(e);
    }
}
