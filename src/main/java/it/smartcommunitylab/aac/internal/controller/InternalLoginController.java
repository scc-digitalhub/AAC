package it.smartcommunitylab.aac.internal.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.ExtendedAuthenticationManager;
import it.smartcommunitylab.aac.core.SessionManager;
import it.smartcommunitylab.aac.core.auth.ProviderWrappedAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.UserAuthenticationToken;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.internal.InternalIdentityAuthority;
import it.smartcommunitylab.aac.internal.InternalUserManager;
import it.smartcommunitylab.aac.internal.dto.InternalLoginBean;
import it.smartcommunitylab.aac.internal.provider.InternalAuthenticationProvider;
import springfox.documentation.annotations.ApiIgnore;

//@Controller
//@RequestMapping
@Deprecated
public class InternalLoginController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private InternalIdentityAuthority internalAuthority;

    @Autowired
    private ExtendedAuthenticationManager authManager;

    @Autowired
    private SessionManager sessionManager;

//    @Autowired
//    private InternalUserManager userManager;

//    @Autowired
//    private InternalAuthenticationProvider authProvider;

    private String buildLoginPage(String realm, String providerId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/login");
        if (!SystemKeys.REALM_GLOBAL.equals(realm)) {
            builder.queryParam("r", realm);
        }
        if (!"".equals(providerId)) {
            builder.queryParam("p", providerId);
        }

        return builder.build().toUriString();
    }

    private String buildLoginAction(String realm, String providerId) {
        StringBuilder builder = new StringBuilder();
        if (!SystemKeys.REALM_GLOBAL.equals(realm)) {
            builder.append("/").append(realm);
        }
        builder.append("/login/internal");
        if (!"".equals(providerId)) {
            builder.append("/").append(providerId);
        }

        return builder.toString();
    }

//    @RequestMapping(value = {
//            "/login/internal",
//            "/{realm}/login/internal",
//            "/{realm}/login/internal/{providerId}"
//    }, method = RequestMethod.GET)
//    public String loginDispatch(
//            @PathVariable("realm") Optional<String> realmKey,
//            @PathVariable("providerId") Optional<String> providerKey,
//            RedirectAttributes attr,
//            HttpServletRequest req, HttpServletResponse res) throws Exception {
//        String realm = SystemKeys.REALM_GLOBAL;
//        String providerId = "";
//
//        // fetch realm+provider
//        if (realmKey.isPresent()) {
//            realm = realmKey.get();
//        }
//        if (providerKey.isPresent()) {
//            providerId = providerKey.get();
//        }
////
////        // todo verify idp + cleanup
//        //
//        // use redirect with params, can refresh
//        return "redirect:" + buildLoginPage(realm, providerId);
//
//        // use model, cant refresh
////        String loginAction = buildLoginAction(realm, providerId);
////        attr.addFlashAttribute("loginAction", loginAction);
////        attr.addFlashAttribute("realm", realm);
////        attr.addFlashAttribute("provider", providerId);
//
////        return "redirect:/login";
//
//    }
//
//    @RequestMapping(value = "/login", method = RequestMethod.GET)
//    public String login(
//            @RequestParam("r") Optional<String> realmKey,
//            @RequestParam("p") Optional<String> providerKey,
//            Model model,
//            HttpServletRequest req, HttpServletResponse res) throws Exception {
//
//        String realm = SystemKeys.REALM_GLOBAL;
//        String providerId = "";
//
//        // fetch realm+provider
//        if (realmKey.isPresent()) {
//            realm = realmKey.get();
//        }
//        if (providerKey.isPresent()) {
//            providerId = providerKey.get();
//        }
//
//        if (!model.containsAttribute("loginAction")) {
//            model.addAttribute("realm", realm);
//            model.addAttribute("provider", providerId);
//            model.addAttribute("loginAction", buildLoginAction(realm, providerId));
//        }
//
////
////        Map<String, Object> model = new HashMap<String, Object>();
////        model.put("realm", realm);
////        model.put("provider", providerId);
////        model.put("loginPath", loginPath);
////        model.put("loginText", loginPath);
//
//        model.addAttribute("loginText", model.getAttribute("loginAction"));
//
//        Map<String, String> authorities = new HashMap<>();
//        authorities.put("internal", "internal");
//        logger.debug("authorities from adapter: " + authorities.keySet().toString());
//        req.getSession().setAttribute("authorities", authorities);
//
////        return new ModelAndView("login", model);
//        return "login";
//    }

    @ApiIgnore
    @RequestMapping(value = {
            "/login/internal",
            "/{realm}/login/internal",
            "/{realm}/login/internal/{providerId}"
    }, method = RequestMethod.POST)
    public String login(
            @PathVariable("realm") Optional<String> realmKey,
            @PathVariable("providerId") Optional<String> providerKey,
            @ModelAttribute @Valid InternalLoginBean login,
            final BindingResult binding, RedirectAttributes attr, Model model,
            HttpServletRequest req) {

        String realm = SystemKeys.REALM_GLOBAL;
        String providerId = "";

        if (login.getRealm() != null) {
            realm = login.getRealm();
        }

        if (login.getProvider() != null) {
            providerId = login.getProvider();
        }

        // fetch realm+provider
        if (realmKey.isPresent()) {
            realm = realmKey.get();
        }
        if (providerKey.isPresent()) {
            providerId = providerKey.get();
        }

        String username = login.getUsername();
        String password = login.getPassword();

        String loginPage = buildLoginPage(realm, providerId);
        String loginPath = buildLoginAction(realm, providerId);

        try {

            // validate
            IdentityProvider idp = internalAuthority.getIdentityProvider(providerId);
            if (idp == null || !idp.getRealm().equals(realm)) {
                // mismatch, this idp does not belong to the realm
                throw new AuthenticationServiceException("invalid provider");
            }

            // we should validate and cleanup username, password
            //

            // check target
            // TODO sanitize
            String target = (String) req.getSession().getAttribute("redirect");
            try {
                if (!StringUtils.hasText(target)) {
                    target = "/";
                }
                target = URLEncoder.encode(target, "UTF8");
            } catch (UnsupportedEncodingException e) {
                throw new RegistrationException(e);
            }

            // build auth token for validation
            UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(
                    username, password);

            // get provider for realm and wrap
            ProviderWrappedAuthenticationToken wrappedAuthRequest = new ProviderWrappedAuthenticationToken(authRequest,
                    providerId, SystemKeys.AUTHORITY_INTERNAL);

            // use authManager, we could leverage internal idp to access auth provider
            Authentication authResponse = authManager.authenticate(wrappedAuthRequest);

            // we got a userAuthToken
            UserAuthenticationToken authentication = (UserAuthenticationToken) authResponse;

////            // replace in session
//            sessionManager.setSession(authentication);

            // merge session
//            sessionManager.mergeSession(authentication);

//            // perform auth via provider
//            RealmAwareUsernamePasswordAuthenticationToken authResponse = (RealmAwareUsernamePasswordAuthenticationToken) authProvider
//                    .authenticate(authRequest);
//
//            if (!authResponse.isAuthenticated()) {
//                // something wrong
//                throw new BadCredentialsException("Bad credentials");
//            }
//
//            // we need to resolve principal
//            String subjectId = userManager.getSubject(realm, username);
//            Subject subject = new Subject(subjectId, username);
//
//            // fetch authorities
//            Collection<GrantedAuthority> authorities = authResponse.getAuthorities();
//            // fetch full identity
//            UserIdentity identity = userManager.getIdentity(realm, username);
//
//            // translate token
//            // we will also clear credentials
//            authResponse.eraseCredentials();
//            ProviderWrappedAuthenticationToken token = new ProviderWrappedAuthenticationToken(authResponse.getRealm(),
//                    SystemKeys.AUTHORITY_INTERNAL, SystemKeys.AUTHORITY_INTERNAL,
//                    authResponse);
//
//            // update session and global authentication
//            UserAuthenticationToken authentication = sessionManager.getUserAuthentication();
//            if (authentication == null || !authentication.getSubject().equals(subject)) {
//                // create as new
//                authentication = new UserAuthenticationToken(subject,
//                        token,
//                        identity, authorities);
//            } else {
//                // attach new identity and token
//                authentication.getUser().addIdentity(identity);
//                authentication.addAuthentication(token);
//            }
//
//            // replace in session
//            sessionManager.setSession(authentication);

            logger.trace("authentication set to " + authentication.toString());
            // do NOT pass email to eauth, we will use the SecurityContext to fetch user
            // avoid impersonation attack
//            String redirect = String
//                    .format("forward:%s",
//                            target);
//            return redirect;
            return "redirect:/whoami";
        } catch (AuthenticationException e) {
//            // ensure at least internal authority is available for login
//            // TODO rewrite, see down
//            Map<String, String> authorities = attributesAdapter.getWebAuthorityUrls();
//            req.getSession().setAttribute("authorities", authorities);

            attr.addFlashAttribute("error", e.getMessage());
            return "redirect:" + loginPage;

//            //send redirect to login to ensure session attributes are set
//            req.getSession().setAttribute("error", e.getClass().getSimpleName());
//            // we should ensure that redirect is fetched via GET to avoid loops
//            // disabled now for compatibility, use 302
//            req.setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, HttpStatus.SEE_OTHER);
//            return "redirect:/login";

//        } catch (NoSuchUserException e) {
//            model.addAttribute("error", e.getMessage());
//            return "login";
        }
    }

//    @ApiIgnore
//    @RequestMapping(value = "/login", method = RequestMethod.POST)
//    public String login(
//            Model model,
//            @RequestParam String username,
//            @RequestParam String password,
//            HttpServletRequest req) {
//        try {
//            Registration user = regService.getUser(username, password);
//            String target = (String) req.getSession().getAttribute("redirect");
//            try {
//                if (!StringUtils.hasText(target)) {
//                    target = "/";
//                }
//                target = URLEncoder.encode(target, "UTF8");
//            } catch (UnsupportedEncodingException e) {
//                throw new RegistrationException(e);
//            }
//
//            List<GrantedAuthority> list = new LinkedList<>();
//            list.add(new SimpleGrantedAuthority(Config.R_USER));
//
//            AbstractAuthenticationToken a = new UsernamePasswordAuthenticationToken(username, password, list);
//            a.setDetails(Config.IDP_INTERNAL);
//
//            SecurityContextHolder.getContext().setAuthentication(a);
//            req.setAttribute("email", user.getEmail());
//
//            logger.trace("authentication set to " + a.toString());
//            // do NOT pass email to eauth, we will use the SecurityContext to fetch user
//            // avoid impersonation attack
//            String redirect = String
//                    .format("forward:/eauth/internal?target=%s",
//                            target);
//            return redirect;
//        } catch (RegistrationException e) {
//            // ensure at least internal authority is available for login
//            // TODO rewrite, see down
//            Map<String, String> authorities = attributesAdapter.getWebAuthorityUrls();
//            req.getSession().setAttribute("authorities", authorities);
//
//            model.addAttribute("error", e.getClass().getSimpleName());
//            return "login";
////            //send redirect to login to ensure session attributes are set
////            req.getSession().setAttribute("error", e.getClass().getSimpleName());
////            // we should ensure that redirect is fetched via GET to avoid loops
////            // disabled now for compatibility, use 302
////            req.setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, HttpStatus.SEE_OTHER);
////            return "redirect:/login";
//
//        }
//    }

//    @ApiIgnore
//    @RequestMapping(value = "/login", method = RequestMethod.POST)
//    public String login(
//            Model model,
//            @RequestParam String username,
//            @RequestParam String password,
//            HttpServletRequest req) {
//        try {
//            Registration user = regService.getUser(username, password);
//            String target = (String) req.getSession().getAttribute("redirect");
//            try {
//                if (!StringUtils.hasText(target)) {
//                    target = "/";
//                }
//                target = URLEncoder.encode(target, "UTF8");
//            } catch (UnsupportedEncodingException e) {
//                throw new RegistrationException(e);
//            }
//
//            List<GrantedAuthority> list = new LinkedList<>();
//            list.add(new SimpleGrantedAuthority(Config.R_USER));
//
//            AbstractAuthenticationToken a = new UsernamePasswordAuthenticationToken(username, password, list);
//            a.setDetails(Config.IDP_INTERNAL);
//
//            SecurityContextHolder.getContext().setAuthentication(a);
//            req.setAttribute("email", user.getEmail());
//
//            logger.trace("authentication set to " + a.toString());
//            // do NOT pass email to eauth, we will use the SecurityContext to fetch user
//            // avoid impersonation attack
//            String redirect = String
//                    .format("forward:/eauth/internal?target=%s",
//                            target);
//            return redirect;
//        } catch (RegistrationException e) {
//            // ensure at least internal authority is available for login
//            // TODO rewrite, see down
//            Map<String, String> authorities = attributesAdapter.getWebAuthorityUrls();
//            req.getSession().setAttribute("authorities", authorities);
//
//            model.addAttribute("error", e.getClass().getSimpleName());
//            return "login";
////            //send redirect to login to ensure session attributes are set
////            req.getSession().setAttribute("error", e.getClass().getSimpleName());
////            // we should ensure that redirect is fetched via GET to avoid loops
////            // disabled now for compatibility, use 302
////            req.setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, HttpStatus.SEE_OTHER);
////            return "redirect:/login";
//
//        }
//    }

//    @RequestMapping("/auth/internal-oauth/callback")
//    public ModelAndView loginOAuth(HttpServletRequest req) throws Exception {
//
//        return new ModelAndView("redirect:/eauth/internal");
//    }
}
