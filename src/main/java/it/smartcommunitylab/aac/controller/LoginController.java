package it.smartcommunitylab.aac.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.ProviderManager;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;

@Controller
@RequestMapping
public class LoginController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ProviderManager providerManager;

    @RequestMapping(value = {
            "/login",
            "/-/{realm}/login",
            "/-/{realm}/login/{providerId}"
    }, method = RequestMethod.GET)
    public String login(
            @PathVariable("realm") Optional<String> realmKey,
            @PathVariable("providerId") Optional<String> providerKey,
            Model model,
            HttpServletRequest req, HttpServletResponse res) throws Exception {

        String realm = SystemKeys.REALM_GLOBAL;
        String providerId = "";

        // fetch realm+provider
        if (realmKey.isPresent()) {
            realm = realmKey.get();
        }
        if (providerKey.isPresent()) {
            providerId = providerKey.get();
        }

        // fetch providers for given realm
        Collection<IdentityProvider> providers = providerManager.getIdentityProviders(realm);

        if (StringUtils.hasText(providerId)) {
            IdentityProvider idp = providerManager.getIdentityProvider(providerId);
            if (idp.getRealm().equals(realm)) {
                providers = Collections.singleton(idp);
            }
        }

        // fetch as authorities model
        // TODO make an helper to format
        List<LoginAuthorityBean> authorities = new ArrayList<>();
        LoginAuthorityBean internal = null;
        for (IdentityProvider idp : providers) {
            LoginAuthorityBean a = new LoginAuthorityBean();
            a.authority = idp.getAuthority();
            a.provider = idp.getProvider();
            a.realm = idp.getRealm();
            a.loginUrl = idp.getAuthenticationUrl();
//            a.name = idp.getName();
            a.name = idp.getName();
            String key = a.name.trim()
                    .replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
            a.cssClass = "provider-" + key;
            a.icon = "key";

            if (ArrayUtils.contains(icons, key)) {
                a.icon = key;
            }

            if (SystemKeys.AUTHORITY_INTERNAL.equals(a.authority)) {
                internal = a;
            } else {

                if (StringUtils.hasText(a.loginUrl)) {
                    authorities.add(a);
                }
            }
        }

        Collections.sort(authorities);

        if (internal != null) {
            model.addAttribute("internalAuthority", internal);
        }

        model.addAttribute("externalAuthorities", authorities);

//
//        Map<String, Object> model = new HashMap<String, Object>();
//        model.put("realm", realm);
//        model.put("provider", providerId);
//        model.put("loginPath", loginPath);
//        model.put("loginText", loginPath);

        model.addAttribute("loginText", model.getAttribute("loginAction"));

//        return new ModelAndView("login", model);
        return "login";
    }

    private String[] icons = {
            "twitter", "facebook", "github"
    };

    private class LoginAuthorityBean implements Comparable {
        public String provider;
        public String authority;
        public String realm;
        public String loginUrl;
        public String icon;
        public String name;
        public String cssClass;

        @Override
        public int compareTo(Object o) {
            if (o instanceof LoginAuthorityBean) {
                return name.compareTo(((LoginAuthorityBean) o).name);
            } else {
                return 0;
            }
        }

    }

}
