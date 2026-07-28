package it.smartcommunitylab.aac.otp.controller;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.LoginException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.core.auth.ProviderWrappedAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.WebAuthenticationDetails;
import it.smartcommunitylab.aac.internal.auth.InternalAuthenticationException;
import it.smartcommunitylab.aac.internal.model.InternalLoginProvider;
import it.smartcommunitylab.aac.otp.OtpCredentialsAuthority;
import it.smartcommunitylab.aac.otp.OtpIdentityAuthority;
import it.smartcommunitylab.aac.otp.auth.UsernameOtpAuthenticationToken;
import it.smartcommunitylab.aac.otp.provider.OtpCredentialsService;
import it.smartcommunitylab.aac.otp.provider.OtpIdentityProvider;
import java.util.Collections;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class OtpLoginController {

    public static final String LOGIN_FORM_URL = OtpIdentityAuthority.AUTHORITY_URL + "form/{providerId}";

    private OtpIdentityAuthority internalAuthority;
    private OtpCredentialsAuthority credentialsAuthority;

    private AuthenticationManager authenticationManager;

    @Autowired
    public void setInternalAuthority(
        OtpIdentityAuthority internalAuthority,
        OtpCredentialsAuthority credentialsAuthority,
        AuthenticationManager authenticationManager
    ) {
        this.internalAuthority = internalAuthority;
        this.credentialsAuthority = credentialsAuthority;
        this.authenticationManager = authenticationManager;
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

                // Use constructor for unauthenticated token
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
                // Ensure authentication details are set
                wrappedToken.setAuthenticationDetails(new WebAuthenticationDetails(req));
                otpAuthRequest.setDetails(new WebAuthenticationDetails(req));

                Authentication authenticatedUser = authenticationManager.authenticate(wrappedToken);
                SecurityContextHolder.getContext().setAuthentication(authenticatedUser);

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

    @RequestMapping(value = LOGIN_FORM_URL, method = RequestMethod.GET)
    public String login(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
        Model model,
        Locale locale,
        HttpServletRequest req,
        HttpServletResponse res
    ) throws Exception {
        OtpIdentityProvider idp = internalAuthority.getProvider(providerId);
        model.addAttribute("providerId", providerId);
        String realm = idp.getRealm();
        model.addAttribute("realm", realm);
        model.addAttribute("displayName", realm);

        InternalLoginProvider a = idp.getLoginProvider(null, null);
        String form = idp.getLoginForm();
        if (form == null) throw new IllegalArgumentException("unsupported-operation");
        a.setTemplate(form);
        a.setLoginUrl(idp.getLoginUrl());
        model.addAttribute("authorities", Collections.singleton(a));

        Exception error = (Exception) req.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        if (error instanceof InternalAuthenticationException) {
            LoginException le = LoginException.translate((InternalAuthenticationException) error);
            model.addAttribute("error", le.getError());
            model.addAttribute("errorMessage", le.getMessage());
            req.getSession().removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        }

        return "login";
    }
}
