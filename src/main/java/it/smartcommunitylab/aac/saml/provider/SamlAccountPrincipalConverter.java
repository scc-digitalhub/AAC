package it.smartcommunitylab.aac.saml.provider;

import java.io.Serializable;
import java.util.Map;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.SamlAttributesSet;
import it.smartcommunitylab.aac.attributes.mapper.SamlAttributesMapper;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.model.AttributeSet;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.provider.AccountPrincipalConverter;
import it.smartcommunitylab.aac.core.provider.UserAccountService;
import it.smartcommunitylab.aac.saml.model.SamlUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.saml.persistence.SamlUserAccount;

@Transactional
public class SamlAccountPrincipalConverter extends AbstractProvider<SamlUserAccount>
        implements AccountPrincipalConverter<SamlUserAccount> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected final UserAccountService<SamlUserAccount> accountService;
    protected final String repositoryId;

    protected final SamlIdentityProviderConfig config;

    // attributes
    private final SamlAttributesMapper samlMapper;

    public SamlAccountPrincipalConverter(String providerId,
            UserAccountService<SamlUserAccount> accountService,
            SamlIdentityProviderConfig config,
            String realm) {
        this(SystemKeys.AUTHORITY_SAML, providerId, accountService, config, realm);
    }

    public SamlAccountPrincipalConverter(String authority, String providerId,
            UserAccountService<SamlUserAccount> accountService,
            SamlIdentityProviderConfig config,
            String realm) {
        super(authority, providerId, realm);
        Assert.notNull(accountService, "account service is mandatory");
        Assert.notNull(config, "provider config is mandatory");

        this.config = config;
        this.accountService = accountService;

        // repositoryId is always providerId, saml isolates data per provider
        this.repositoryId = providerId;

        // build mapper with default config for parsing attributes
        this.samlMapper = new SamlAttributesMapper();
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

    @Override
    public SamlUserAccount convertAccount(UserAuthenticatedPrincipal userPrincipal, String userId) {
        // we expect an instance of our model
        Assert.isInstanceOf(SamlUserAuthenticatedPrincipal.class, userPrincipal,
                "principal must be an instance of saml authenticated principal");
        SamlUserAuthenticatedPrincipal principal = (SamlUserAuthenticatedPrincipal) userPrincipal;

        // we use upstream subject for accounts
        // TODO handle transient ids, for example with session persistence
        String subjectId = principal.getSubjectId();

        // attributes from provider
        String username = principal.getUsername();
        Map<String, Serializable> attributes = principal.getAttributes();

        // map attributes to saml set and flatten to string
        // we also clean every attribute and allow only plain text
        AttributeSet samlAttributeSet = samlMapper.mapAttributes(attributes);
        Map<String, String> samlAttributes = samlAttributeSet.getAttributes()
                .stream()
                .collect(Collectors.toMap(
                        a -> a.getKey(),
                        a -> a.exportValue()));

        String email = clean(samlAttributes.get(SamlAttributesSet.EMAIL));
        username = StringUtils.hasText(samlAttributes.get(SamlAttributesSet.USERNAME))
                ? clean(samlAttributes.get(SamlAttributesSet.USERNAME))
                : principal.getUsername();

        // update additional attributes
        String issuer = samlAttributes.get("issuer");
        if (!StringUtils.hasText(issuer)) {
            issuer = config.getRelyingPartyRegistration().getAssertingPartyDetails().getEntityId();
        }

        String name = StringUtils.hasText(samlAttributes.get(SamlAttributesSet.NAME))
                ? clean(samlAttributes.get(SamlAttributesSet.NAME))
                : username;
        String surname = clean(samlAttributes.get(SamlAttributesSet.SURNAME));

        boolean defaultVerifiedStatus = config.getConfigMap().getTrustEmailAddress() != null
                ? config.getConfigMap().getTrustEmailAddress()
                : false;
        boolean emailVerified = StringUtils.hasText(samlAttributes.get(SamlAttributesSet.EMAIL_VERIFIED))
                ? Boolean.parseBoolean(samlAttributes.get(SamlAttributesSet.EMAIL_VERIFIED))
                : defaultVerifiedStatus;

        if (Boolean.TRUE.equals(config.getConfigMap().getAlwaysTrustEmailAddress())) {
            emailVerified = true;
        }

        // build model from scratch
        SamlUserAccount account = new SamlUserAccount(getAuthority());
        account.setRepositoryId(repositoryId);
        account.setProvider(getProvider());

        account.setSubjectId(subjectId);
        account.setUserId(userId);
        account.setRealm(getRealm());

        account.setUsername(username);
        account.setIssuer(issuer);
        account.setName(name);
        account.setSurname(surname);
        account.setEmail(email);
        account.setEmailVerified(emailVerified);

        // also add all principal attributes for persistence
        account.setAttributes(attributes);

        return account;
    }

    private String clean(String input) {
        return clean(input, Safelist.none());
    }

    private String clean(String input, Safelist safe) {
        if (StringUtils.hasText(input)) {
            return Jsoup.clean(input, safe);
        }
        return null;

    }
}
