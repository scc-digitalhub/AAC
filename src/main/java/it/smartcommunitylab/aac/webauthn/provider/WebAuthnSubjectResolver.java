package it.smartcommunitylab.aac.webauthn.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.webauthn.persistence.WebAuthnUserAccount;
import it.smartcommunitylab.aac.webauthn.service.WebAuthnUserAccountService;

public class WebAuthnSubjectResolver extends AbstractProvider
        implements SubjectResolver<WebAuthnUserAccount> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public final static String[] ATTRIBUTES = { "email" };

    private final WebAuthnUserAccountService accountService;
    private final WebAuthnIdentityProviderConfig config;

    public WebAuthnSubjectResolver(String providerId, WebAuthnUserAccountService userAccountService,
            WebAuthnIdentityProviderConfig providerConfig, String realm) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, providerId, realm);
        Assert.notNull(userAccountService, "user account service is mandatory");

        this.accountService = userAccountService;
        this.config = providerConfig;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_SUBJECT;
    }

    @Override
    public Subject resolveByUsername(String username) {
        logger.debug("resolve by username " + username);
        WebAuthnUserAccount account = accountService.findAccountByUsername(getProvider(), username);
        if (account == null) {
            return null;
        }
        // build subject with username
        return new Subject(account.getUserId(), getRealm(), account.getUsername(), SystemKeys.RESOURCE_USER);
    }

    @Override
    public Subject resolveByAccountId(String username) {
        // accountId is username
        return resolveByUsername(username);
    }

    @Override
    public Subject resolveByPrincipalId(String username) {
        // principalId is username
        return resolveByUsername(username);
    }

    @Override
    public Subject resolveByIdentityId(String username) {
        // identityId is username
        return resolveByUsername(username);
    }

    @Override
    public Subject resolveByEmailAddress(String email) {
        // DISABLED
        // TODO evaluate if webauthn is linkable
        return null;

//        if (!config.isLinkable()) {
//            return null;
//        }
//
//        logger.debug("resolve by email " + email);
//        WebAuthnUserAccount account = accountService.findAccountByEmailAddress(getProvider(), email).stream()
//                .filter(a -> a.isEmailVerified())
//                .findFirst()
//                .orElse(null);
//
//        if (account == null) {
//            return null;
//        }
//
//        // build subject with username
//        return new Subject(account.getUserId(), getRealm(), account.getUsername(), SystemKeys.RESOURCE_USER);
    }

}
