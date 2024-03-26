package it.smartcommunitylab.aac.spid.provider;

import it.smartcommunitylab.aac.base.provider.AbstractProvider;
import it.smartcommunitylab.aac.identity.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.identity.provider.AccountPrincipalConverter;
import it.smartcommunitylab.aac.spid.model.SpidUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Transactional
public class SpidAccountPrincipalConverter
    extends AbstractProvider<SpidUserAccount>
    implements AccountPrincipalConverter<SpidUserAccount> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final SpidIdentityProviderConfig config;
    private final String repositoryId;

    public SpidAccountPrincipalConverter(
        String authority,
        String providerId,
        SpidIdentityProviderConfig config,
        String realm
    ) {
        super(authority, providerId, realm);
        this.config = config;
        // TODO: probabilmente questo è ERRATO: non è detto che in SPID i dati siano isolati per providerId
        this.repositoryId = providerId;
    }

    @Override
    public SpidUserAccount convertAccount(UserAuthenticatedPrincipal userPrincipal, String userId) {
        Assert.isInstanceOf(
                SpidUserAuthenticatedPrincipal.class,
                userPrincipal,
                "principal must be an instance of saml authenticated principal"
        );
        SpidUserAccount account = new SpidUserAccount(getProvider(), getRealm(), null);

        SpidUserAuthenticatedPrincipal principal = (SpidUserAuthenticatedPrincipal) userPrincipal;

        account.setRepositoryId(repositoryId);
        account.setSubjectId(principal.getSubjectId());
        account.setUserId(principal.getUserId());

        // TODO: handle missing attributes
        account.setUsername(principal.getUsername());
        account.setName(principal.getName());
        account.setEmail(principal.getEmailAddress());
        account.setSpidCode(principal.getSpidCode());
        return account;
    }
}
