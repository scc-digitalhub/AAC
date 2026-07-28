package it.smartcommunitylab.aac.otp.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.credentials.base.AbstractCredentialsService;
import it.smartcommunitylab.aac.credentials.persistence.UserCredentialsService;
import it.smartcommunitylab.aac.internal.model.InternalUserAccount;
import it.smartcommunitylab.aac.internal.service.InternalJpaUserAccountService;
import it.smartcommunitylab.aac.otp.model.InternalEditableUserOtp;
import it.smartcommunitylab.aac.otp.model.InternalUserOtp;
import it.smartcommunitylab.aac.otp.persistence.InternalUserOtpEntity;
import it.smartcommunitylab.aac.otp.persistence.InternalUserOtpEntityRepository;
import it.smartcommunitylab.aac.utils.MailService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.mail.MessagingException;
import org.springframework.util.Assert;

public class OtpCredentialsService
    extends AbstractCredentialsService<
        InternalUserOtp,
        InternalEditableUserOtp,
        OtpIdentityProviderConfigMap,
        OtpCredentialsServiceConfig
    > {

    private final InternalUserOtpEntityRepository otpRepository;
    private final InternalJpaUserAccountService accountService;
    private final String repositoryId;

    private MailService mailService;
    private RealmAwareUriBuilder uriBuilder;

    public OtpCredentialsService(
        String providerId,
        UserCredentialsService<InternalUserOtp> credentialsService,
        InternalJpaUserAccountService accountService,
        OtpCredentialsServiceConfig providerConfig,
        InternalUserOtpEntityRepository otpRepository,
        String realm
    ) {
        super(SystemKeys.AUTHORITY_OTP, providerId, credentialsService, providerConfig, realm);
        Assert.notNull(accountService, "accountService is mandatory");
        Assert.notNull(otpRepository, "otpRepository is mandatory");
        this.accountService = accountService;
        this.otpRepository = otpRepository;
        this.repositoryId = providerConfig.getRepositoryId() != null ? providerConfig.getRepositoryId() : providerId;
    }

    public void setMailService(MailService mailService) {
        this.mailService = mailService;
    }

    public void setUriBuilder(RealmAwareUriBuilder uriBuilder) {
        this.uriBuilder = uriBuilder;
    }

    /**
     * Generates and sends OTP.
     */
    public void generateOtp(String username, String providerId) throws RegistrationException, NoSuchUserException {
        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        // 1. Controlla se l'utente ha già un OTP valido non ancora scaduto
        List<InternalUserOtpEntity> existingOtps = otpRepository.findByUserId(account.getUserId());
        Long now = System.currentTimeMillis();

        if (!existingOtps.isEmpty()) {
            InternalUserOtpEntity lastOtp = existingOtps.get(0);
            if (lastOtp.getExpiryTimestamp() != null && lastOtp.getExpiryTimestamp() > now && !lastOtp.isConsumed()) {
                throw new RegistrationException("otp-already-generated");
            }
        }

        // 2. Prepara il DTO dell'OTP
        String otpId = UUID.randomUUID().toString();
        String code = UUID.randomUUID().toString();
        Long expiryTime = now + TimeUnit.MINUTES.toMillis(5);

        InternalUserOtp otpDto = new InternalUserOtp(account.getRealm(), otpId);
        otpDto.setRepositoryId(repositoryId);
        otpDto.setUserId(account.getUserId());
        otpDto.setToken(code);
        otpDto.setExpiryTimestamp(expiryTime);
        otpDto.setAttempts(0);
        otpDto.setConsumed(false);
        otpDto.setProvider(providerId);

        InternalUserOtp savedOtp;
        try {
            savedOtp = credentialsService.addCredentials(repositoryId, otpId, otpDto);
        } catch (RegistrationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException(e.getMessage());
        }

        // 4. Invia l'email con il codice generato
        try {
            InternalUserOtpEntity accountOtpEntity = otpRepository.findOne(savedOtp.getId());
            sendOtpMail(accountOtpEntity, account, code, account.getLang());
        } catch (MessagingException e) {
            throw new SystemException(e.getMessage());
        }
    }

    private void sendOtpMail(InternalUserOtpEntity accountOtp, InternalUserAccount account, String code, String lang)
        throws MessagingException {
        if (mailService != null) {
            Map<String, Object> vars = new HashMap<>();
            vars.put("code", code);
            vars.put("user", account);

            String link = "";
            if (uriBuilder != null) {
                link = uriBuilder.buildUrl(null, "/auth/otp/verify/" + accountOtp.getProviderId() + "/" + code);
            }
            vars.put("link", link);

            Map<String, Object> action = new HashMap<>();
            action.put("url", link);
            action.put("text", "action.login");
            vars.put("action", action);

            vars.put("realm", getRealm());

            mailService.sendEmail(account.getEmail(), "otp", lang, vars);
        }
    }

    public boolean verifyOtp(String token, String providerId) throws NoSuchUserException {
        if (token == null || token.isEmpty() || providerId == null || providerId.isEmpty()) {
            return false;
        }

        InternalUserOtpEntity accountOtp = otpRepository.findByTokenAndProviderId(token, getProvider());
        Long now = System.currentTimeMillis();

        if (
            accountOtp != null &&
            providerId.equals(accountOtp.getProviderId()) &&
            !accountOtp.isConsumed() &&
            accountOtp.getExpiryTimestamp() != null &&
            accountOtp.getExpiryTimestamp() > now &&
            accountOtp.getAttempts() < 3
        ) {
            // Segna come consumato una volta verificato con successo
            accountOtp.setConsumed(true);
            otpRepository.saveAndFlush(accountOtp);
            return true;
        }

        if (accountOtp != null && accountOtp.getAttempts() < 3) {
            // Incrementa i tentativi falliti
            accountOtp.setAttempts(accountOtp.getAttempts() + 1);
            otpRepository.saveAndFlush(accountOtp);
        }

        return false;
    }

    @Override
    public String getRegisterUrl() {
        return uriBuilder != null ? uriBuilder.buildUrl(getRealm(), "/otp/register") : null;
    }

    @Override
    public String getEditUrl(String credentialsId) {
        return uriBuilder != null ? uriBuilder.buildUrl(getRealm(), "/otp/edit/" + credentialsId) : null;
    }
}
