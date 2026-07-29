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
import java.security.SecureRandom;
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

    private static final String CHARACTERS = "abcdefghijklmoqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final int CODE_LENGTH = 6;
    private static final int VALIDITY_PERIOD = 2;

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

        List<InternalUserOtpEntity> existingOtps = otpRepository.findByUserId(account.getUserId());
        Long now = System.currentTimeMillis();

        if (!existingOtps.isEmpty()) {
            //TODO: Check how many tokens are still valid made the user be able to generate max 10
        }

        String otpId = UUID.randomUUID().toString();
        String code = generateOtpCodeString(CODE_LENGTH);
        Long expiryTime = now + TimeUnit.MINUTES.toMillis(VALIDITY_PERIOD);

        InternalUserOtp otpDto = new InternalUserOtp(account.getRealm(), otpId);
        otpDto.setRepositoryId(repositoryId);
        otpDto.setUserId(account.getUserId());
        otpDto.setToken(code);
        otpDto.setExpiryTimestamp(expiryTime);
        otpDto.setProvider(providerId);

        InternalUserOtp savedOtp;
        try {
            savedOtp = credentialsService.addCredentials(repositoryId, otpId, otpDto);
        } catch (RegistrationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException(e.getMessage());
        }

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
            accountOtp != null && providerId.equals(accountOtp.getProviderId()) && accountOtp.getExpiryTimestamp() > now
            //TODO: check attempts globally for the account, not per token
        ) {
            return true;
        }

        if (accountOtp != null && providerId.equals(accountOtp.getProviderId())) {
            //TODO: increase attempts globally for the account
        }

        return false;
    }

    public static String generateOtpCodeString(int length) {
        return RANDOM.ints(length, 0, CHARACTERS.length())
            .mapToObj(CHARACTERS::charAt)
            .collect(StringBuilder::new, (sb, ch) -> sb.append(ch), (sb1, sb2) -> sb1.append(sb2))
            .toString();
    }

    public String getUserIdForToken(String token) {
        InternalUserOtpEntity accountOtp = otpRepository.findByTokenAndProviderId(token, getProvider());
        return accountOtp != null ? accountOtp.getUserId() : null;
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
