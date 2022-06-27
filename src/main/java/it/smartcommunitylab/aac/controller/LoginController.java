package it.smartcommunitylab.aac.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.LoginException;
import it.smartcommunitylab.aac.core.AuthorityManager;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.RealmManager;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.service.ClientDetailsService;
import it.smartcommunitylab.aac.dto.CustomizationBean;
import it.smartcommunitylab.aac.dto.LoginProvider;
import it.smartcommunitylab.aac.model.Realm;

@Controller
@RequestMapping
public class LoginController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${application.name}")
    private String applicationName;

    @Autowired
    private AuthorityManager authorityManager;

    @Autowired
    private RealmManager realmManager;

    @Autowired
    private ClientDetailsService clientDetailsService;

    // TODO handle COMMON realm
    @RequestMapping(value = {
            "/login"
    }, method = RequestMethod.GET)
    public String entrypoint(
            @RequestParam(required = false, name = "realm") Optional<String> realmKey,
            @RequestParam(required = false, name = "client_id") Optional<String> clientKey,
            Model model,
            HttpServletRequest req, HttpServletResponse res) throws Exception {

        if (clientKey.isPresent()) {
            String clientId = clientKey.get();

            // lookup client realm and dispatch redirect
            ClientDetails clientDetails = clientDetailsService.loadClient(clientId);
            String realm = clientDetails.getRealm();

            String redirect = "/-/" + realm + "/login?client_id=" + clientId;
            return "redirect:" + redirect;
        }

        if (realmKey.isPresent() && StringUtils.hasText(realmKey.get())) {
            String realm = realmKey.get();
            String redirect = "/-/" + realm + "/login";
            return "redirect:" + redirect;
        }

        // load (public) realm list and present select page
        List<Realm> realms = new ArrayList<>(realmManager.listRealms(true));

        Collections.sort(realms, new Comparator<Realm>() {
            @Override
            public int compare(Realm o1, Realm o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        model.addAttribute("displayName", applicationName);
        model.addAttribute("realms", realms);
        model.addAttribute("loginUrl", "/login");

        return "entrypoint";
    }

    @RequestMapping(value = {
            "/login"
    }, method = RequestMethod.POST)
    public String redirect(
            @RequestParam(required = true, name = "realm") String realmKey,
            HttpServletRequest req, HttpServletResponse res) throws Exception {

        if (StringUtils.hasText(realmKey)) {
            Realm realm = realmManager.findRealm(realmKey);
            if (realm != null) {
                String slug = realm.getSlug();
                String redirect = "/-/" + slug + "/login";
                return "redirect:" + redirect;
            }
        }

        return "redirect:/login";
    }

    @RequestMapping(value = {
            "/-/{realm}/login",
            "/-/{realm}/login/{providerId}"
    }, method = RequestMethod.GET)
    public String login(
            @PathVariable("realm") Optional<String> realmKey,
            @PathVariable("providerId") Optional<String> providerKey,
            @RequestParam(required = false, name = "client_id") Optional<String> clientKey,
            Model model,
            HttpServletRequest req, HttpServletResponse res) throws Exception {

        // TODO handle /login as COMMON login, ie any realm is valid
        String realm = SystemKeys.REALM_SYSTEM;
        String providerId = "";
        String clientId = null;

        // fetch realm+provider
        if (realmKey.isPresent()) {
            realm = realmKey.get();
        }
        if (providerKey.isPresent()) {
            providerId = providerKey.get();
        }
        if (clientKey.isPresent()) {
            clientId = clientKey.get();
        }

        if (!StringUtils.hasText(realm)) {
            throw new IllegalArgumentException("no suitable realm for login");
        }

        model.addAttribute("realm", realm);

        String displayName = applicationName;
        Realm re = null;
        Map<String, String> resources = new HashMap<>();
        if (!realm.equals(SystemKeys.REALM_COMMON)) {
            re = realmManager.getRealm(realm);
            displayName = re.getName();
            CustomizationBean gcb = re.getCustomization("global");
            if (gcb != null) {
                resources.putAll(gcb.getResources());
            }
            CustomizationBean lcb = re.getCustomization("login");
            if (lcb != null) {
                resources.putAll(lcb.getResources());
            }
        }

        model.addAttribute("displayName", displayName);
        model.addAttribute("customization", resources);

        // fetch providers for given realm
        Collection<IdentityProvider<? extends UserIdentity>> providers = authorityManager
                .getIdentityProviders(realm);

        if (StringUtils.hasText(providerId)) {
            IdentityProvider<? extends UserIdentity> idp = authorityManager
                    .getIdentityProvider(providerId);
            if (idp.getRealm().equals(realm)) {
                providers = Collections.singleton(idp);
            }
        }

        // fetch client if provided
        if (clientId != null) {
            ClientDetails clientDetails = clientDetailsService.loadClient(clientId);
            model.addAttribute("client", clientDetails);

            // check realm and providers
            // TODO evaluate enforcing realm (or common) match
            if (clientDetails.getRealm().equals(realm)) {
                providers = providers.stream().filter(p -> clientDetails.getProviders().contains(p.getProvider()))
                        .collect(Collectors.toList());
            }
        }

        // fetch as authorities model
        List<LoginProvider> authorities = new ArrayList<>();
        for (IdentityProvider<? extends UserIdentity> idp : providers) {
            LoginProvider a = idp.getLoginProvider();
            authorities.add(a);
        }

        // bypass idp selection when only 1 is available

        if (authorities.size() == 1) {
            LoginProvider lab = authorities.get(0);
            // note: we can bypass only providers which expose a button,
            // anything else requires user interaction
//            if (SystemKeys.DISPLAY_MODE_BUTTON.equals(lab.getDisplayMode())) {
            String redirectUrl = lab.getLoginUrl();
            logger.trace("bypass login for single idp, send to " + redirectUrl);
            return "redirect:" + redirectUrl;
//            }

        }

        // sort by name
        Collections.sort(authorities);

        // build a display list respecting display mode for ordering: form, spid, button
        // TODO rework with comparable on model
        List<LoginProvider> loginAuthorities = new ArrayList<>();
        loginAuthorities.addAll(authorities.stream()
                .filter(a -> "form".equals(a.getTemplate()))
                .collect(Collectors.toList()));
        loginAuthorities.addAll(authorities.stream()
                .filter(a -> "spid".equals(a.getTemplate()))
                .collect(Collectors.toList()));
        loginAuthorities.addAll(authorities.stream()
                .filter(a -> "button".equals(a.getTemplate()))
                .collect(Collectors.toList()));

        model.addAttribute("authorities", loginAuthorities);

        // check errors
        Exception error = (Exception) req.getSession()
                .getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        if (error != null && error instanceof AuthenticationException) {
            LoginException le = LoginException.translate((AuthenticationException) error);

            model.addAttribute("error", le.getError());
            model.addAttribute("errorMessage", le.getMessage());

            // also remove from session
            req.getSession().removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        }

        return "login";
    }

    @RequestMapping(value = {
            "/-/{realm}", "/-/{realm}/"
    }, method = RequestMethod.GET)
    public String realm(
            @PathVariable("realm") String realm,
            Authentication authentication) throws Exception {

        if (authentication == null) {
            return "redirect:/-/" + realm + "/login";
        }

        return "redirect:/";
    }

}
