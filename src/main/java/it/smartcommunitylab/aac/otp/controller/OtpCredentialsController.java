package it.smartcommunitylab.aac.otp.controller;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.internal.model.InternalUserAccount;
import it.smartcommunitylab.aac.otp.OtpCredentialsAuthority;
import it.smartcommunitylab.aac.otp.OtpIdentityAuthority;
import it.smartcommunitylab.aac.otp.provider.OtpCredentialsService;
import it.smartcommunitylab.aac.otp.provider.OtpIdentityProvider;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@Validated
public class OtpCredentialsController {

    private final OtpCredentialsAuthority credentialsAuthority;
    private final OtpIdentityAuthority identityAuthority;

    public OtpCredentialsController(
        OtpCredentialsAuthority credentialsAuthority,
        OtpIdentityAuthority identityAuthority
    ) {
        this.credentialsAuthority = credentialsAuthority;
        this.identityAuthority = identityAuthority;
    }

    @PostMapping(value = "/auth/otp/request/{providerId}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> requestOtp(
        @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
        @RequestParam(required = false) String username,
        @RequestParam(required = false) String email
    ) throws RegistrationException, SystemException {
        if (!StringUtils.hasText(username) && !StringUtils.hasText(email)) {
            return ResponseEntity.badRequest().build();
        }

        try {
            OtpCredentialsService service = credentialsAuthority.getProvider(providerId);
            if (service == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            String resolvedUsername = StringUtils.hasText(username)
                ? username
                : resolveUsernameByEmail(providerId, email);
            if (!StringUtils.hasText(resolvedUsername)) {
                return ResponseEntity.badRequest().build();
            }

            service.generateOtp(resolvedUsername, providerId);
            return ResponseEntity.noContent().build();
        } catch (NoSuchProviderException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (NoSuchUserException e) {
            return ResponseEntity.badRequest().build();
        } catch (RegistrationException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        } catch (SystemException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String resolveUsernameByEmail(String providerId, String email) throws NoSuchProviderException {
        if (!StringUtils.hasText(email)) {
            return null;
        }

        OtpIdentityProvider idp = identityAuthority.getProvider(providerId);
        if (idp == null) {
            throw new NoSuchProviderException("Otp provider not found");
        }

        InternalUserAccount account = idp.getAccountProvider().findAccountByEmail(email);
        if (account == null) {
            return null;
        }

        return account.getUsername();
    }
}
