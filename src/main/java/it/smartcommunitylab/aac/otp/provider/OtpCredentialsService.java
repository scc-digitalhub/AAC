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
import org.springframework.util.StringUtils;

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
    private static final int VALIDITY_PERIOD = 1;

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

    public void generateOtp(String username, String providerId) throws RegistrationException, NoSuchUserException {
        InternalUserAccount account = accountService.findAccountById(repositoryId, username);
        if (account == null) {
            throw new NoSuchUserException();
        }

        List<InternalUserOtpEntity> existingOtps = otpRepository.findByUserId(account.getUserId());
        long now = System.currentTimeMillis();

        long validCount = existingOtps.stream().filter(o -> o.getExpiryTimestamp() > now).count();
        if (validCount >= 10) {
            throw new RegistrationException("otp.max_tokens_exceeded");
        }

        String otpId = UUID.randomUUID().toString();
        String code = generateOtpCodeString(CODE_LENGTH);
        long expiryTime = now + TimeUnit.MINUTES.toMillis(VALIDITY_PERIOD);

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
        if (mailService == null) {
            return;
        }

        Map<String, Object> vars = new HashMap<>();
        vars.put("code", code);
        vars.put("user", account);

        String link = (uriBuilder != null)
            ? uriBuilder.buildUrl(
                null,
                "/auth/otp/verify/" + accountOtp.getProviderId() + "/" + accountOtp.getUserId() + "/" + code
            )
            : "";

        vars.put("link", link);
        vars.put("action", Map.of("url", link, "text", "action.login"));
        vars.put("realm", getRealm());

        mailService.sendEmail(account.getEmail(), "otp", lang, vars);
    }

    public void consumeOtp(String token, String providerId, String userId) {
        InternalUserOtpEntity accountOtp = otpRepository.findByTokenAndProviderIdAndUserId(token, providerId, userId);
        if (accountOtp != null) {
            otpRepository.delete(accountOtp);
        }
    }

    public boolean verifyOtp(String token, String providerId, String userId) {
        if (!StringUtils.hasText(token) || !StringUtils.hasText(providerId) || !StringUtils.hasText(userId)) {
            return false;
        }

        InternalUserOtpEntity accountOtp = otpRepository.findByTokenAndProviderId(token, providerId);

        String userIdFromToken = getUserIdForToken(token);

        if (userIdFromToken == null || !userIdFromToken.equals(userId)) {
            return false;
        }

        if (accountOtp == null || !providerId.equals(accountOtp.getProviderId())) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (accountOtp.getExpiryTimestamp() > now) {
            // TODO: check attempts globally for the account, not per token
            return true;
        }

        // TODO: increase attempts globally if failed verification, not per token but per account
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
