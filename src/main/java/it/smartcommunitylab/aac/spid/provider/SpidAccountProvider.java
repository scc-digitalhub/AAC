package it.smartcommunitylab.aac.spid.provider;

import java.util.List;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.common.MissingDataException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.provider.AccountProvider;
import it.smartcommunitylab.aac.core.service.SubjectService;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.model.UserStatus;
import it.smartcommunitylab.aac.spid.model.SpidUserAttribute;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccount;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccountId;
import it.smartcommunitylab.aac.spid.persistence.SpidUserAccountRepository;

@Transactional
public class SpidAccountProvider extends AbstractProvider implements AccountProvider<SpidUserAccount> {

    private final SpidUserAccountRepository accountRepository;
    private final SpidIdentityProviderConfig config;

    private final SubjectService subjectService;

    protected SpidAccountProvider(String providerId, SpidUserAccountRepository accountRepository,
            SubjectService subjectService,
            SpidIdentityProviderConfig config,
            String realm) {
        super(SystemKeys.AUTHORITY_SPID, providerId, realm);
        Assert.notNull(accountRepository, "account repository is mandatory");
        Assert.notNull(subjectService, "subject service is mandatory");
        Assert.notNull(config, "provider config is mandatory");

        this.config = config;
        this.accountRepository = accountRepository;
        this.subjectService = subjectService;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpidUserAccount> listAccounts(String userId) {
        List<SpidUserAccount> accounts = accountRepository.findByUserIdAndProvider(userId, getProvider());

        // we need to detach
        return accounts.stream().map(a -> {
            return accountRepository.detach(a);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SpidUserAccount getAccount(String subjectId) throws NoSuchUserException {
        SpidUserAccount account = findAccountBySubjectId(subjectId);
        if (account == null) {
            throw new NoSuchUserException();
        }

        return account;
    }

    @Transactional(readOnly = true)
    public SpidUserAccount findAccount(String subjectId) {
        return findAccountBySubjectId(subjectId);
    }

    @Transactional(readOnly = true)
    public SpidUserAccount findAccountBySubjectId(String subjectId) {
        String provider = getProvider();

        SpidUserAccount account = accountRepository.findOne(new SpidUserAccountId(provider, subjectId));
        if (account == null) {
            return null;
        }

        // detach the entity, we don't want modifications to be persisted via a
        // read-only interface
        // for example eraseCredentials will reset the password in db
        return accountRepository.detach(account);
    }

    @Override
    @Transactional(readOnly = true)
    public SpidUserAccount findAccountByUuid(String uuid) {
        String provider = getProvider();

        SpidUserAccount account = accountRepository.findByProviderAndUuid(provider, uuid);
        if (account == null) {
            return null;
        }

        // detach the entity, we don't want modifications to be persisted via a
        // read-only interface
        // for example eraseCredentials will reset the password in db
        return accountRepository.detach(account);
    }

    @Override
    public void deleteAccount(String subjectId) throws NoSuchUserException {
        SpidUserAccount account = findAccountBySubjectId(subjectId);

        if (account != null) {
            String uuid = account.getUuid();
            if (uuid != null) {
                // remove subject if exists
                subjectService.deleteSubject(uuid);
            }

            accountRepository.delete(account);
        }
    }

    @Override
    public SpidUserAccount lockAccount(String sub) throws NoSuchUserException, RegistrationException {
        return updateStatus(sub, UserStatus.LOCKED);
    }

    @Override
    public SpidUserAccount unlockAccount(String sub) throws NoSuchUserException, RegistrationException {
        return updateStatus(sub, UserStatus.ACTIVE);
    }

    @Override
    public SpidUserAccount linkAccount(String sub, String userId)
            throws NoSuchUserException, RegistrationException {

        // we expect userId to be valid
        if (!StringUtils.hasText(userId)) {
            throw new MissingDataException("user");
        }

        String provider = getProvider();

        SpidUserAccount account = accountRepository.findOne(new SpidUserAccountId(provider, sub));
        if (account == null) {
            throw new NoSuchUserException();
        }

        // check if active, inactive accounts can not be changed except for activation
        UserStatus curStatus = UserStatus.parse(account.getStatus());
        if (UserStatus.INACTIVE == curStatus) {
            throw new IllegalArgumentException("account is inactive, activate first to update status");
        }

        // re-link to user
        account.setUserId(userId);
        account = accountRepository.save(account);
        return accountRepository.detach(account);
    }

    /*
     * operations
     */

    public SpidUserAccount registerAccount(String userId, SpidUserAccount reg)
            throws NoSuchUserException, RegistrationException {
        String provider = getProvider();

        // we expect userId to be valid
        if (!StringUtils.hasText(userId)) {
            throw new MissingDataException("user");
        }

        // check if already registered
        String subjectId = clean(reg.getSubjectId());
        SpidUserAccount account = accountRepository.findOne(new SpidUserAccountId(provider, subjectId));
        if (account != null) {
            throw new AlreadyRegisteredException();
        }

        String realm = getRealm();

        // extract id fields
        String spidCode = clean(reg.getSpidCode());
        String email = clean(reg.getEmail());
        String username = clean(reg.getUsername());
        String phone = clean(reg.getPhone());
        String fiscalNumber = clean(reg.getFiscalNumber());
        String ivaCode = clean(reg.getIvaCode());

        if (SpidUserAttribute.EMAIL == config.getIdAttribute() && !StringUtils.hasText(email)) {
            throw new MissingDataException("email");
        }
        if (SpidUserAttribute.USERNAME == config.getIdAttribute() && !StringUtils.hasText(username)) {
            throw new MissingDataException("username");
        }
        if (SpidUserAttribute.SPID_CODE == config.getIdAttribute() && !StringUtils.hasText(spidCode)) {
            throw new MissingDataException("spid-code");
        }
        if (SpidUserAttribute.MOBILE_PHONE == config.getIdAttribute() && !StringUtils.hasText(phone)) {
            throw new MissingDataException("mobile-phone");
        }
        if (SpidUserAttribute.FISCAL_NUMBER == config.getIdAttribute() && !StringUtils.hasText(fiscalNumber)) {
            throw new MissingDataException("fiscal-number");
        }
        if (SpidUserAttribute.IVA_CODE == config.getIdAttribute() && !StringUtils.hasText(ivaCode)) {
            throw new MissingDataException("iva-code");
        }

        // validate
        if (!StringUtils.hasText(subjectId)) {
            throw new MissingDataException("subject-identifier");
        }

        // extract attributes
        String idp = reg.getIdp();
        String name = clean(reg.getName());
        String surname = clean(reg.getSurname());

        // generate uuid and register as subject
        String uuid = subjectService.generateUuid(SystemKeys.RESOURCE_ACCOUNT);
        Subject s = subjectService.addSubject(uuid, realm, SystemKeys.RESOURCE_ACCOUNT, subjectId);

        account = new SpidUserAccount();
        account.setProvider(provider);
        account.setSubjectId(subjectId);
        account.setUuid(s.getSubjectId());

        account.setUserId(userId);
        account.setRealm(realm);

        account.setIdp(idp);
        account.setUsername(username);
        account.setEmail(email);
        account.setPhone(phone);
        account.setSpidCode(spidCode);
        account.setFiscalNumber(fiscalNumber);
        account.setIvaCode(ivaCode);
        account.setName(name);
        account.setSurname(surname);

        // set account as active
        account.setStatus(UserStatus.ACTIVE.getValue());

        account = accountRepository.save(account);
        return accountRepository.detach(account);
    }

    public SpidUserAccount updateAccount(String sub, SpidUserAccount reg)
            throws NoSuchUserException, RegistrationException {
        String provider = getProvider();

        SpidUserAccount account = accountRepository.findOne(new SpidUserAccountId(provider, sub));
        if (account == null) {
            throw new NoSuchUserException();
        }

        // check if active, inactive accounts can not be changed except for activation
        UserStatus curStatus = UserStatus.parse(account.getStatus());
        if (UserStatus.INACTIVE == curStatus) {
            throw new IllegalArgumentException("account is inactive, activate first to update status");
        }

        // id attributes
        String spidCode = clean(reg.getSpidCode());
        String email = clean(reg.getEmail());
        String username = clean(reg.getUsername());
        String phone = clean(reg.getPhone());
        String fiscalNumber = clean(reg.getFiscalNumber());
        String ivaCode = clean(reg.getIvaCode());

        if (SpidUserAttribute.EMAIL == config.getIdAttribute() && !StringUtils.hasText(email)) {
            throw new MissingDataException("email");
        }
        if (SpidUserAttribute.USERNAME == config.getIdAttribute() && !StringUtils.hasText(username)) {
            throw new MissingDataException("username");
        }
        if (SpidUserAttribute.SPID_CODE == config.getIdAttribute() && !StringUtils.hasText(spidCode)) {
            throw new MissingDataException("spid-code");
        }
        if (SpidUserAttribute.MOBILE_PHONE == config.getIdAttribute() && !StringUtils.hasText(phone)) {
            throw new MissingDataException("mobile-phone");
        }
        if (SpidUserAttribute.FISCAL_NUMBER == config.getIdAttribute() && !StringUtils.hasText(fiscalNumber)) {
            throw new MissingDataException("fiscal-number");
        }
        if (SpidUserAttribute.IVA_CODE == config.getIdAttribute() && !StringUtils.hasText(ivaCode)) {
            throw new MissingDataException("iva-code");
        }

        // extract attributes
        String idp = reg.getIdp();
        String name = clean(reg.getName());
        String surname = clean(reg.getSurname());

        account.setIdp(idp);
        account.setUsername(username);
        account.setEmail(email);
        account.setPhone(phone);
        account.setSpidCode(spidCode);
        account.setFiscalNumber(fiscalNumber);
        account.setIvaCode(ivaCode);
        account.setName(name);
        account.setSurname(surname);

        account = accountRepository.save(account);
        return accountRepository.detach(account);
    }

    private SpidUserAccount updateStatus(String sub, UserStatus newStatus)
            throws NoSuchUserException, RegistrationException {
        String provider = getProvider();

        SpidUserAccount account = accountRepository.findOne(new SpidUserAccountId(provider, sub));
        if (account == null) {
            throw new NoSuchUserException();
        }

        // check if active, inactive accounts can not be changed except for activation
        UserStatus curStatus = UserStatus.parse(account.getStatus());
        if (UserStatus.INACTIVE == curStatus && UserStatus.ACTIVE != newStatus) {
            throw new IllegalArgumentException("account is inactive, activate first to update status");
        }

        // update status
        account.setStatus(newStatus.getValue());
        account = accountRepository.save(account);
        return accountRepository.detach(account);
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
