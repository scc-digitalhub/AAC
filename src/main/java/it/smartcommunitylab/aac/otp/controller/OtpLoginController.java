package it.smartcommunitylab.aac.otp.controller;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.core.auth.ProviderWrappedAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;
import it.smartcommunitylab.aac.internal.auth.InternalAuthenticationException;
import it.smartcommunitylab.aac.otp.OtpCredentialsAuthority;
import it.smartcommunitylab.aac.otp.OtpIdentityAuthority;
import it.smartcommunitylab.aac.otp.auth.UsernameOtpAuthenticationToken;
import it.smartcommunitylab.aac.otp.provider.OtpCredentialsService;
import java.util.Collections;
import javax.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class OtpLoginController {

    private OtpCredentialsAuthority credentialsAuthority;
    private AuthenticationManager authenticationManager;

    private static final String AUTHORITY_URL = OtpIdentityAuthority.AUTHORITY_URL;

    public OtpLoginController(
        OtpCredentialsAuthority credentialsAuthority,
        AuthenticationManager authenticationManager
    ) {
        this.credentialsAuthority = credentialsAuthority;
        this.authenticationManager = authenticationManager;
    }

    @GetMapping(value = AUTHORITY_URL + "/verify/{providerId}/{userId}/{token}")
    public String verify(
        @PathVariable String providerId,
        @PathVariable String userId,
        @PathVariable String token,
        HttpServletRequest req
    ) throws NoSuchProviderException {
        OtpCredentialsService service = credentialsAuthority.getProvider(providerId);

        if (!service.verifyOtp(token, providerId, userId)) {
            handleError(req, "invalid-otp");
            return "redirect:/";
        }

        try {
            String userIdFromToken = service.getUserIdForToken(token);

            if (userIdFromToken == null) throw new InternalAuthenticationException("otp", "user-not-found");

            UsernameOtpAuthenticationToken otpAuthRequest = new UsernameOtpAuthenticationToken(
                userIdFromToken,
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

            service.consumeOtp(token, providerId);

            return "redirect:/";
        } catch (Exception e) {
            handleError(req, "error-verifying-otp: " + e.getMessage());
            return "redirect:/";
        }
    }

    private void handleError(HttpServletRequest req, String error) {
        req
            .getSession()
            .setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, new InternalAuthenticationException("otp", error));
    }
}
