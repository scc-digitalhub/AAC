package it.smartcommunitylab.aac.otp.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.accounts.provider.AccountService;
import it.smartcommunitylab.aac.attributes.model.UserAttributes;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityProvider;
import it.smartcommunitylab.aac.internal.model.InternalLoginProvider;
import it.smartcommunitylab.aac.internal.model.InternalUserAccount;
import it.smartcommunitylab.aac.internal.model.InternalUserIdentity;
import it.smartcommunitylab.aac.internal.provider.InternalAccountPrincipalConverter;
import it.smartcommunitylab.aac.internal.provider.InternalAccountProvider;
import it.smartcommunitylab.aac.internal.provider.InternalAttributeProvider;
import it.smartcommunitylab.aac.internal.provider.InternalSubjectResolver;
import it.smartcommunitylab.aac.otp.OtpIdentityAuthority;
import it.smartcommunitylab.aac.otp.model.InternalOtpUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.realms.service.RealmService;
import it.smartcommunitylab.aac.utils.MailService;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.provider.AuthorizationRequest;

public class OtpIdentityProvider
    extends AbstractIdentityProvider<
        InternalUserIdentity,
        InternalUserAccount,
        InternalOtpUserAuthenticatedPrincipal,
        OtpIdentityProviderConfigMap,
        OtpIdentityProviderConfig
    > {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final OtpIdentityCredentialsService otpService;

    private final OtpAuthenticationProvider authenticationProvider;
    private final InternalAccountPrincipalConverter principalConverter;
    private final InternalAccountProvider accountProvider;
    private final InternalAttributeProvider<InternalOtpUserAuthenticatedPrincipal> attributeProvider;
    private final InternalSubjectResolver subjectResolver;

    public OtpIdentityProvider(
        String providerId,
        UserAccountService<InternalUserAccount> userAccountService,
        OtpIdentityProviderConfig config,
        String realm
    ) {
        super(SystemKeys.AUTHORITY_OTP, providerId, config, realm);
        String repositoryId = config.getRepositoryId();
        logger.debug("create otp provider with id {} repository {}", String.valueOf(providerId), repositoryId);

        this.attributeProvider = new InternalAttributeProvider<>(SystemKeys.AUTHORITY_OTP, providerId, realm);

        this.accountProvider = new InternalAccountProvider(
            SystemKeys.AUTHORITY_OTP,
            providerId,
            userAccountService,
            repositoryId,
            realm
        );
        this.principalConverter = new InternalAccountPrincipalConverter(
            SystemKeys.AUTHORITY_OTP,
            providerId,
            userAccountService,
            repositoryId,
            realm
        );

        this.otpService = new OtpIdentityCredentialsService(providerId, userAccountService, config, realm);
        this.authenticationProvider = new OtpAuthenticationProvider(
            providerId,
            userAccountService,
            otpService,
            config,
            realm
        );

        this.subjectResolver = new InternalSubjectResolver(providerId, userAccountService, repositoryId, false, realm);
    }

    public void setRealmService(RealmService rs) {
        otpService.setRealmService(rs);
    }

    public void setMailService(MailService ms) {
        otpService.setMailService(ms);
    }

    public void setUriBuilder(RealmAwareUriBuilder ub) {
        otpService.setUriBuilder(ub);
    }

    @Override
    public boolean isAuthoritative() {
        return false;
    }

    @Override
    protected AccountService<InternalUserAccount, ?, ?, ?> getAccountService() {
        return null;
    }

    @Override
    public OtpAuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    @Override
    public InternalAccountProvider getAccountProvider() {
        return accountProvider;
    }

    @Override
    protected InternalAccountPrincipalConverter getAccountPrincipalConverter() {
        return principalConverter;
    }

    @Override
    protected InternalAttributeProvider<InternalOtpUserAuthenticatedPrincipal> getAttributeProvider() {
        return attributeProvider;
    }

    @Override
    public InternalSubjectResolver getSubjectResolver() {
        return subjectResolver;
    }

    @Override
    protected InternalUserIdentity buildIdentity(
        InternalUserAccount account,
        InternalOtpUserAuthenticatedPrincipal principal,
        Collection<UserAttributes> attributes
    ) {
        InternalUserIdentity identity = new InternalUserIdentity(
            getAuthority(),
            getProvider(),
            getRealm(),
            account,
            principal
        );
        identity.setAttributes(attributes);
        return identity;
    }

    @Override
    public void deleteIdentity(String userId, String username) throws NoSuchUserException {}

    @Override
    public String getAuthenticationUrl() {
        return getFormUrl();
    }

    public String getLoginUrl() {
        // we use an address bound to provider, no reason to expose realm
        return OtpIdentityAuthority.AUTHORITY_URL + "login/" + getProvider();
    }

    public String getFormUrl() {
        return OtpIdentityAuthority.AUTHORITY_URL + "form/" + getProvider();
    }

    public String getLoginForm() {
        return "otp_form";
    }

    @Override
    public InternalLoginProvider getLoginProvider(ClientDetails clientDetails, AuthorizationRequest authRequest) {
        InternalLoginProvider ilp = new InternalLoginProvider(getProvider(), getRealm(), getName());

        ilp.setTitleMap(getTitleMap());
        ilp.setDescriptionMap(getDescriptionMap());

        ilp.setLoginUrl(getLoginUrl());

        ilp.setFormUrl(getFormUrl());

        String template = config.displayAsButton() ? "button" : getLoginForm();
        ilp.setTemplate(template);

        ilp.setPosition(getConfig().getPosition());

        return ilp;
    }
}
