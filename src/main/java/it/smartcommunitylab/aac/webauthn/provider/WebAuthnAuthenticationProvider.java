package it.smartcommunitylab.aac.webauthn.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.webauthn.model.WebAuthnUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccount;

public class WebAuthnAuthenticationProvider
        extends ExtendedAuthenticationProvider<WebAuthnUserAuthenticatedPrincipal, WebAuthnUserAccount> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

}
