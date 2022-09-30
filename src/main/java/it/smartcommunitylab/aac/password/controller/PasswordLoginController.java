package it.smartcommunitylab.aac.password.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.LoginException;
import it.smartcommunitylab.aac.core.RealmManager;
import it.smartcommunitylab.aac.internal.auth.InternalAuthenticationException;
import it.smartcommunitylab.aac.internal.model.InternalLoginProvider;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.password.PasswordIdentityAuthority;
import it.smartcommunitylab.aac.password.provider.PasswordIdentityProvider;

@Controller
public class PasswordLoginController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    public static final String LOGIN_FORM_URL = PasswordIdentityAuthority.AUTHORITY_URL + "form/{providerId}";

    @Autowired
    private PasswordIdentityAuthority internalAuthority;

    @Autowired
    private RealmManager realmManager;

    @RequestMapping(value = LOGIN_FORM_URL, method = RequestMethod.GET)
    public String login(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            Model model,
            HttpServletRequest req, HttpServletResponse res) throws Exception {
        // resolve provider
        PasswordIdentityProvider idp = internalAuthority.getProvider(providerId);
        model.addAttribute("providerId", providerId);

        String realm = idp.getRealm();
        model.addAttribute("realm", realm);

        Realm re = realmManager.getRealm(realm);
        String displayName = re.getName();
        Map<String, String> resources = new HashMap<>();
//        if (!realm.equals(SystemKeys.REALM_COMMON)) {
//            re = realmManager.getRealm(realm);
//            displayName = re.getName();
//            CustomizationBean gcb = re.getCustomization("global");
//            if (gcb != null) {
//                resources.putAll(gcb.getResources());
//            }
//            // disable realm login customization here,
//            // we have a per idp message where needed
////            CustomizationBean rcb = re.getCustomization("login");
////            if (rcb != null) {
////                resources.putAll(rcb.getResources());
////            }
//        }

        model.addAttribute("displayName", displayName);
        model.addAttribute("customization", resources);

        InternalLoginProvider a = idp.getLoginProvider();
        // make sure we show the form
        // it should also point to login
        String form = idp.getLoginForm();
        if (form == null) {
            throw new IllegalArgumentException("unsupported-operation");
        }
        a.setTemplate(form);
        a.setLoginUrl(idp.getLoginUrl());
        model.addAttribute("authorities", Collections.singleton(a));

        // check errors
        // we make sure we consume only internal exceptions.
        Exception error = (Exception) req.getSession()
                .getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        if (error != null && error instanceof InternalAuthenticationException) {
            LoginException le = LoginException.translate((InternalAuthenticationException) error);

            model.addAttribute("error", le.getError());
            model.addAttribute("errorMessage", le.getMessage());

            // also remove from session
            req.getSession().removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        }

        return "login";
    }

}
