package it.smartcommunitylab.aac.spid.provider;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.provider.SubjectResolver;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.saml.model.SamlUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.spid.model.SpidUserAttribute;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccount;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccountId;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccountRepository;

@Transactional
public class SpidSubjectResolver extends AbstractProvider implements SubjectResolver {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SpidUserAccountRepository accountRepository;
    private final SpidIdentityProviderConfig config;

    protected SpidSubjectResolver(String providerId, SpidUserAccountRepository accountRepository,
            SpidIdentityProviderConfig config,
            String realm) {
        super(SystemKeys.AUTHORITY_SPID, providerId, realm);
        Assert.notNull(accountRepository, "account repository is mandatory");
        Assert.notNull(config, "provider config is mandatory");

        this.accountRepository = accountRepository;
        this.config = config;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_SUBJECT;
    }

    @Transactional(readOnly = true)
    public Subject resolveBySubjectId(String subjectId) {
        logger.debug("resolve by subjectId " + subjectId);
        SpidUserAccount account = accountRepository.findOne(new SpidUserAccountId(getProvider(), subjectId));
        if (account == null) {
            return null;
        }

        // build subject with username
        return new Subject(account.getUserId(), getRealm(), account.getUsername(), SystemKeys.RESOURCE_USER);
    }

    @Override
    public Subject resolveByAccountId(String accountId) {
        // accountId is subjectId
        return resolveBySubjectId(accountId);
    }

    @Override
    public Subject resolveByPrincipalId(String principalId) {
        // principalId is subjectId
        return resolveBySubjectId(principalId);
    }

    @Override
    public Subject resolveByIdentityId(String identityId) {
        // identityId is sub
        return resolveBySubjectId(identityId);
    }

    @Override
    @Transactional(readOnly = true)
    public Subject resolveByAttributes(Map<String, String> attributes) {
        String attributeName = config.getIdAttribute().getValue();

        if (attributes.keySet().contains(attributeName)
                && getRealm().equals((attributes.get("realm")))) {
            // resolve to an account by attribute
            SpidUserAccount account = null;
            if (SpidUserAttribute.SPID_CODE == config.getIdAttribute()) {
                String spidCode = attributes.get(attributeName);
                account = accountRepository.findByProviderAndSpidCode(getProvider(), spidCode).stream().findFirst()
                        .orElse(null);
            } else if (SpidUserAttribute.EMAIL == config.getIdAttribute()) {
                String email = attributes.get(attributeName);
                account = accountRepository.findByProviderAndEmail(getProvider(), email).stream().findFirst()
                        .orElse(null);
            } else if (SpidUserAttribute.USERNAME == config.getIdAttribute()) {
                String username = attributes.get(attributeName);
                account = accountRepository.findByProviderAndUsername(getProvider(), username).stream().findFirst()
                        .orElse(null);
            } else if (SpidUserAttribute.MOBILE_PHONE == config.getIdAttribute()) {
                String phone = attributes.get(attributeName);
                account = accountRepository.findByProviderAndPhone(getProvider(), phone).stream().findFirst()
                        .orElse(null);
            } else if (SpidUserAttribute.FISCAL_NUMBER == config.getIdAttribute()) {
                String fiscalNumber = attributes.get(attributeName);
                account = accountRepository.findByProviderAndFiscalNumber(getProvider(), fiscalNumber).stream()
                        .findFirst()
                        .orElse(null);
            } else if (SpidUserAttribute.IVA_CODE == config.getIdAttribute()) {
                String ivaCode = attributes.get(attributeName);
                account = accountRepository.findByProviderAndIvaCode(getProvider(), ivaCode).stream().findFirst()
                        .orElse(null);
            }

            if (account == null) {
                return null;
            }

            // build subject with username
            return new Subject(account.getUserId(), getRealm(), account.getUsername(), SystemKeys.RESOURCE_USER);
        } else {
            return null;
        }
    }

    @Override
    public Map<String, String> getAttributes(UserAuthenticatedPrincipal principal) {
        if (!config.isLinkable()) {
            return null;
        }

        if (!(principal instanceof SamlUserAuthenticatedPrincipal)) {
            return null;
        }

        SamlUserAuthenticatedPrincipal user = (SamlUserAuthenticatedPrincipal) principal;
        Map<String, String> attributes = new HashMap<>();
        attributes.put("realm", getRealm());
        // export userId
        attributes.put("userId", user.getUserId());

        if (SpidUserAttribute.EMAIL == config.getIdAttribute() && user.isEmailVerified()) {
            // export email
            attributes.put("email", user.getEmail());
        }

        if (SpidUserAttribute.USERNAME == config.getIdAttribute() && StringUtils.hasText(user.getUsername())) {
            // export username
            attributes.put("username", user.getName());
        }

        // additional id attributes
        Map<String, String> principalAttributes = user.getAttributes().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().toString()));

        String attributeName = config.getIdAttribute().getValue();
        if (StringUtils.hasText(principalAttributes.get(attributeName))) {
            // export id attribute
            attributes.put(attributeName, principalAttributes.get(attributeName));
        }

        return attributes;
    }

}
