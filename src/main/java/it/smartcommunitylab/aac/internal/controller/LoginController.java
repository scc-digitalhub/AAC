package it.smartcommunitylab.aac.internal.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.Constants;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.Subject;
import it.smartcommunitylab.aac.core.RealmAuthenticationToken;
import it.smartcommunitylab.aac.core.RealmAwareUsernamePasswordAuthenticationToken;
import it.smartcommunitylab.aac.core.SessionManager;
import it.smartcommunitylab.aac.core.UserAuthenticationToken;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.internal.InternalAuthenticationProvider;
import it.smartcommunitylab.aac.internal.InternalUserManager;
import it.smartcommunitylab.aac.internal.InternalUserSubjectResolver;
import it.smartcommunitylab.aac.utils.Utils;
import springfox.documentation.annotations.ApiIgnore;

@Controller
@RequestMapping
public class LoginController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private InternalUserManager userManager;

    @Autowired
    private InternalAuthenticationProvider authProvider;

    @RequestMapping("/login")
    public ModelAndView login(HttpServletRequest req, HttpServletResponse res) throws Exception {
        Map<String, Object> model = new HashMap<String, Object>();
        Map<String, String> authorities = new HashMap<>();
        authorities.put("internal", "internal");
        logger.debug("authorities from adapter: " + authorities.keySet().toString());
        req.getSession().setAttribute("authorities", authorities);

        return new ModelAndView("login", model);
    }

    /**
     * Login the user
     * 
     * @param model
     * @param username
     * @param password
     * @param req
     * @return
     */

    @ApiIgnore
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public String login(
            Model model,
            @RequestParam String username,
            @RequestParam String password,
            HttpServletRequest req) {
        try {
            // TODO fetch realm
            String realm = null;
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
            RealmAwareUsernamePasswordAuthenticationToken authRequest = new RealmAwareUsernamePasswordAuthenticationToken(
                    realm, username, password);

            // perform auth via provider
            RealmAwareUsernamePasswordAuthenticationToken authResponse = (RealmAwareUsernamePasswordAuthenticationToken) authProvider
                    .authenticate(authRequest);

            if (!authResponse.isAuthenticated()) {
                // something wrong
                throw new BadCredentialsException("Bad credentials");
            }

            // we need to resolve principal
            String subjectId = userManager.getSubject(realm, username);
            Subject subject = new Subject(subjectId, username);

            // fetch authorities
            Collection<GrantedAuthority> authorities = authResponse.getAuthorities();
            // fetch full identity
            UserIdentity identity = userManager.getIdentity(realm, username);

            // translate token
            // we will also clear credentials
            authResponse.eraseCredentials();
            RealmAuthenticationToken token = new RealmAuthenticationToken(authResponse.getRealm(),
                    Constants.AUTHORITY_INTERNAL, Constants.AUTHORITY_INTERNAL,
                    authResponse);

            // update session and global authentication
            UserAuthenticationToken authentication = sessionManager.getUserAuthentication();
            if (authentication == null || !authentication.getSubject().equals(subject)) {
                // create as new
                authentication = new UserAuthenticationToken(subject,
                        token,
                        identity, authorities);
            } else {
                // attach new identity and token
                authentication.getUser().addIdentity(identity);
                authentication.addAuthentication(token);
            }

            // replace in session
            sessionManager.setSession(authentication);
            logger.trace("authentication set to " + authentication.toString());
            // do NOT pass email to eauth, we will use the SecurityContext to fetch user
            // avoid impersonation attack
            String redirect = String
                    .format("forward:%s",
                            target);
            return redirect;
        } catch (RegistrationException e) {
//            // ensure at least internal authority is available for login
//            // TODO rewrite, see down
//            Map<String, String> authorities = attributesAdapter.getWebAuthorityUrls();
//            req.getSession().setAttribute("authorities", authorities);

            model.addAttribute("error", e.getMessage());
            return "login";
//            //send redirect to login to ensure session attributes are set
//            req.getSession().setAttribute("error", e.getClass().getSimpleName());
//            // we should ensure that redirect is fetched via GET to avoid loops
//            // disabled now for compatibility, use 302
//            req.setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, HttpStatus.SEE_OTHER);
//            return "redirect:/login";

        } catch (NoSuchUserException e) {
            model.addAttribute("error", e.getMessage());
            return "login";
        } catch (AuthenticationException e) {
            model.addAttribute("error", e.getMessage());
            return "login";
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
