package it.smartcommunitylab.aac.otp.controller;

import java.util.Collections;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.core.auth.ProviderWrappedAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;
import it.smartcommunitylab.aac.internal.auth.InternalAuthenticationException;
import it.smartcommunitylab.aac.otp.OtpCredentialsAuthority;
import it.smartcommunitylab.aac.otp.OtpIdentityAuthority;
import it.smartcommunitylab.aac.otp.auth.UsernameOtpAuthenticationToken;
import it.smartcommunitylab.aac.otp.persistence.InternalUserOtpEntity;
import it.smartcommunitylab.aac.otp.persistence.InternalUserOtpEntityRepository;
import it.smartcommunitylab.aac.otp.provider.OtpCredentialsService;

@Controller
public class OtpLoginController {

    public static final String LOGIN_FORM_URL = OtpIdentityAuthority.AUTHORITY_URL + "form/{providerId}";

    private OtpCredentialsAuthority credentialsAuthority;

    private AuthenticationManager authenticationManager;
    private InternalUserOtpEntityRepository otpRepository;

    @Autowired
    public void setInternalAuthority(
        OtpCredentialsAuthority credentialsAuthority,
        AuthenticationManager authenticationManager,
        InternalUserOtpEntityRepository otpRepository
    ) {
        this.credentialsAuthority = credentialsAuthority;
        this.authenticationManager = authenticationManager;
        this.otpRepository = otpRepository;
    }

    @RequestMapping(value = "/auth/otp/verify/{providerId}/{token}", method = RequestMethod.GET)
    public String verify(@PathVariable String providerId, @PathVariable String token, HttpServletRequest req)
        throws NoSuchProviderException {
        OtpCredentialsService service = credentialsAuthority.getProvider(providerId);

        try {
            boolean verified = service.verifyOtp(token, providerId);
            if (verified) {
                // Find user by token
                String userId = service.getUserIdForToken(token);
                if (userId == null) throw new InternalAuthenticationException("otp", "user-not-found");

                UsernameOtpAuthenticationToken otpAuthRequest = new UsernameOtpAuthenticationToken(
                    userId,
                    token,
                    Collections.emptyList()
                );

                ProviderWrappedAuthenticationToken wrappedToken = new ProviderWrappedAuthenticationToken(
                    otpAuthRequest,
                    providerId,
                    SystemKeys.AUTHORITY_OTP
                );

                wrappedToken.setAuthenticationDetails(new WebAuthenticationDetails(req));
                otpAuthRequest.setDetails(new WebAuthenticationDetails(req));

                Authentication authenticatedUser = authenticationManager.authenticate(wrappedToken);
                SecurityContextHolder.getContext().setAuthentication(authenticatedUser);

                // Delete the OTP after successful authentication
                InternalUserOtpEntity otpEntity = otpRepository.findByTokenAndProviderId(token, providerId);
                otpRepository.delete(otpEntity);

                return "redirect:/";
            } else {
                req
                    .getSession()
                    .setAttribute(
                        WebAttributes.AUTHENTICATION_EXCEPTION,
                        new InternalAuthenticationException("otp", "invalid-otp")
                    );
                return "redirect:/";
            }
        } catch (Exception e) {
            req
                .getSession()
                .setAttribute(
                    WebAttributes.AUTHENTICATION_EXCEPTION,
                    new InternalAuthenticationException("otp", "error-verifying-otp: " + e.getMessage())
                );
            return "redirect:/";
        }
    }
}
