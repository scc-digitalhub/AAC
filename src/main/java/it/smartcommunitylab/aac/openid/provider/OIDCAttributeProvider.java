package it.smartcommunitylab.aac.openid.provider;

import java.util.Collection;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.store.AttributeService;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.model.AttributeSet;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.AttributeProvider;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccountRepository;

@Transactional
public class OIDCAttributeProvider extends AbstractProvider implements AttributeProvider {

    private final OIDCUserAccountRepository accountRepository;
    private final AttributeService attributeService;

    protected OIDCAttributeProvider(String provider, OIDCUserAccountRepository accountRepository,
            AttributeService attributeService, String realm) {
        super(SystemKeys.AUTHORITY_OIDC, provider, realm);

        Assert.notNull(accountRepository, "accountRepository is mandatory");
//        Assert.notNull(attributeService, "attribute service is mandatory");

        this.accountRepository = accountRepository;
        this.attributeService = attributeService;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ATTRIBUTES;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<AttributeSet> listCustomAttributeSets() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<String> listCustomAttributes(String setId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean canProvide(String globalSetId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public UserAttributes provideAttributes(UserIdentity identity, String globalSetId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<UserAttributes> convertAttributes(UserIdentity identity) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<UserAttributes> convertAttributes(Collection<UserAttributes> attributes) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserAttributes convertAttributes(UserAttributes attributes) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<UserAttributes> getUserAttributes(String userId) throws NoSuchUserException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public UserAttributes getUserAttributes(String userId, String setId) throws NoSuchUserException {
        // TODO Auto-generated method stub
        return null;
    }

}
