package it.smartcommunitylab.aac.auth.internal;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.manager.AttributesAdapter;
import it.smartcommunitylab.aac.manager.RegistrationService;
import it.smartcommunitylab.aac.model.Registration;
import springfox.documentation.annotations.ApiIgnore;

@Controller
@RequestMapping
public class LoginController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private RegistrationService regService;

    @Autowired
    private AttributesAdapter attributesAdapter;

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
            Registration user = regService.getUser(username, password);
            String target = (String) req.getSession().getAttribute("redirect");
            try {
                if (!StringUtils.hasText(target)) {
                    target = "/";
                }
                target = URLEncoder.encode(target, "UTF8");
            } catch (UnsupportedEncodingException e) {
                throw new RegistrationException(e);
            }

            List<GrantedAuthority> list = new LinkedList<>();
            list.add(new SimpleGrantedAuthority(Config.R_USER));

            AbstractAuthenticationToken a = new UsernamePasswordAuthenticationToken(username, password, list);
            a.setDetails(Config.IDP_INTERNAL);

            SecurityContextHolder.getContext().setAuthentication(a);
            req.setAttribute("email", user.getEmail());

            logger.trace("authentication set to " + a.toString());
            // do NOT pass email to eauth, we will use the SecurityContext to fetch user
            // avoid impersonation attack
            String redirect = String
                    .format("forward:/eauth/internal?target=%s",
                            target);
            return redirect;
        } catch (RegistrationException e) {
            // ensure at least internal authority is available for login
            // TODO rewrite, see down
            Map<String, String> authorities = attributesAdapter.getWebAuthorityUrls();
            req.getSession().setAttribute("authorities", authorities);

            model.addAttribute("error", e.getClass().getSimpleName());
            return "login";
//            //send redirect to login to ensure session attributes are set
//            req.getSession().setAttribute("error", e.getClass().getSimpleName());
//            // we should ensure that redirect is fetched via GET to avoid loops
//            // disabled now for compatibility, use 302
//            req.setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, HttpStatus.SEE_OTHER);
//            return "redirect:/login";

        }
    }

    @RequestMapping("/auth/internal-oauth/callback")
    public ModelAndView loginOAuth(HttpServletRequest req) throws Exception {

        return new ModelAndView("redirect:/eauth/internal");
    }
}
