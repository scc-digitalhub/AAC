package it.smartcommunitylab.aac.openid.apple.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import it.smartcommunitylab.aac.attributes.store.AttributeStore;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccountRepository;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProvider;

public class AppleIdentityProvider extends OIDCIdentityProvider {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // provider configuration
    private final AppleIdentityProviderConfig config;

    // providers
    protected final AppleAuthenticationProvider authenticationProvider;

    public AppleIdentityProvider(String providerId, OIDCUserAccountRepository accountRepository,
            AttributeStore attributeStore, SubjectService subjectService, AppleIdentityProviderConfig config,
            String realm) {
        super(providerId, accountRepository, attributeStore, subjectService, config.toOidcProviderConfig(), realm);
        this.config = config;

        // build custom authenticator
        this.authenticationProvider = new AppleAuthenticationProvider(providerId, accountRepository, config, realm);

    }

    @Override
    public AppleAuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

}
