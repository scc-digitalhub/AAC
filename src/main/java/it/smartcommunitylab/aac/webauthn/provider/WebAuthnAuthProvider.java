package it.smartcommunitylab.aac.webauthn.provider;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.webauthn.auth.WebAuthnAutentication;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnCredential;

@Service
public class WebAuthnAuthProvider implements AuthenticationProvider, InitializingBean, MessageSourceAware {

    // @Autowired
    // private WebAuthnAccountService webAuthnAccountService;

    protected MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();

    @Override
    public void setMessageSource(MessageSource messageSource) {
        this.messages = new MessageSourceAccessor(messageSource);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // Assert.notNull(this.webAuthnAccountService, "A WebAuthn account service must
        // be set");
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!supports(authentication.getClass())) {
            return null;
        }
        final WebAuthnAutentication auth = (WebAuthnAutentication) authentication;
        final WebAuthnCredential cred = auth.getCredentials();

        // TODO: civts,
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return WebAuthnAutentication.class.isAssignableFrom(authentication);
    }

}
