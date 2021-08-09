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

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.ProviderManager;
import it.smartcommunitylab.aac.core.RealmManager;
import it.smartcommunitylab.aac.core.base.ConfigurableProperties;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityService;
import it.smartcommunitylab.aac.core.service.ClientDetailsService;
import it.smartcommunitylab.aac.dto.CustomizationBean;
import it.smartcommunitylab.aac.model.Realm;

@Controller
@RequestMapping
public class LoginController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${application.name}")
    private String applicationName;

    @Autowired
    private ProviderManager providerManager;

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
        Collection<IdentityProvider> providers = providerManager.getIdentityProviders(realm);

        if (StringUtils.hasText(providerId)) {
            IdentityProvider idp = providerManager.getIdentityProvider(providerId);
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
            a.description = idp.getDescription();
            String key = a.name.trim()
                    .replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
            a.cssClass = "provider-" + key;
            a.icon = "it-key";

            if (ArrayUtils.contains(icons, key)) {
                a.icon = "logo-" + key;
            }
            a.iconUrl = a.icon.startsWith("logo-") ? "svg/sprite.svg#" + a.icon : "italia/svg/sprite.svg#" + a.icon;

            a.fragment = idp.getLoginComponent() != null ? idp.getLoginComponent() : "button";
            a.configuration = idp.getConfiguration();

            if (SystemKeys.AUTHORITY_INTERNAL.equals(idp.getAuthority())) {
                internal = a;
                // also lookup internal service for registration/reset links
                IdentityService ids = providerManager.fetchIdentityService(idp.getAuthority(), idp.getProvider());
                if (ids != null) {
                    if (ids.canRegister() && StringUtils.hasText(ids.getRegistrationUrl())) {
                        a.registrationUrl = ids.getRegistrationUrl();
                    }

                    if (ids.getCredentialsService() != null
                            && ids.getCredentialsService().canReset()
                            && StringUtils.hasText(ids.getCredentialsService().getResetUrl())) {
                        a.resetUrl = ids.getCredentialsService().getResetUrl();
                    }

                }

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

    private String[] icons = {
            "google", "facebook", "github", "microsoft", "apple", "instagram"
    };

    private class LoginAuthorityBean implements Comparable<LoginAuthorityBean> {
        public String provider;
        public String authority;
        public String realm;
        public String loginUrl;
        public String registrationUrl;
        public String resetUrl;
        public String icon;
        public String iconUrl;
        public String name;
        public String description;
        public String cssClass;
        public String fragment;
        public ConfigurableProperties configuration;

        @Override
        public int compareTo(LoginAuthorityBean o) {
            return name.compareTo(((LoginAuthorityBean) o).name);
        }

    }

}
