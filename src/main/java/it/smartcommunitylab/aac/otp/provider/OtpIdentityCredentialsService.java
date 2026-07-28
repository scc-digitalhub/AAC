package it.smartcommunitylab.aac.otp.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.base.provider.AbstractProvider;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.internal.model.InternalUserAccount;
import it.smartcommunitylab.aac.otp.model.InternalUserOtp;
import it.smartcommunitylab.aac.realms.service.RealmService;
import it.smartcommunitylab.aac.utils.MailService;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class OtpIdentityCredentialsService extends AbstractProvider<InternalUserOtp> {

    private final UserAccountService<InternalUserAccount> accountService;
    private final String repositoryId;

    private MailService mailService;
    private RealmAwareUriBuilder uriBuilder;
    private RealmService realmService;

    private static final int MAGIC_LINK_EXPIRY = 300000; // 5 minutes in milliseconds

    public OtpIdentityCredentialsService(
        String providerId,
        UserAccountService<InternalUserAccount> accountService,
        OtpIdentityProviderConfig config,
        String realm
    ) {
        super(SystemKeys.AUTHORITY_OTP, providerId, realm);
        this.accountService = accountService;
        this.repositoryId = config.getRepositoryId();
    }

    public void setMailService(MailService ms) {
        this.mailService = ms;
    }

    public void setUriBuilder(RealmAwareUriBuilder ub) {
        this.uriBuilder = ub;
    }

    public void setRealmService(RealmService rs) {
        this.realmService = rs;
    }

    public void sendMagicLink(String userId) throws Exception {
        // Fetch user account to determine language
        InternalUserAccount account = accountService.findAccountById(repositoryId, userId);
        String lang = (account != null && org.springframework.util.StringUtils.hasText(account.getLang()))
            ? account.getLang()
            : "en";

        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Long expiry = System.currentTimeMillis() + MAGIC_LINK_EXPIRY;

        InternalUserOtp otp = new InternalUserOtp(getRealm(), userId);
        otp.setToken(token);
        otp.setExpiryTimestamp(expiry);

        String link = uriBuilder.buildUri(getRealm(), "otp/verify/" + token).toString();

        Map<String, String> action = new HashMap<>();
        action.put("url", link);
        action.put("text", "action.login");

        Map<String, String> application = new HashMap<>();
        String realm = getRealm();
        application.put("name", realm);
        application.put("url", uriBuilder.buildUrl(realm, "/login"));
        application.put("logo", uriBuilder.buildUrl(realm, "/logo"));
        application.put("email", "");

        if (realmService != null) {
            it.smartcommunitylab.aac.model.Realm r = realmService.findRealm(realm);
            if (r != null) {
                application.put("name", r.getName());
                application.put("email", Optional.ofNullable(r.getEmail()).orElse(""));
            }
        }

        Map<String, Object> vars = new HashMap<>();
        vars.put("user", account);
        vars.put("action", action);
        vars.put("realm", realm);
        vars.put("application", application);

        mailService.sendEmail(userId, "otp", lang, vars);
    }
}
