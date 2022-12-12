package it.smartcommunitylab.aac.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import it.smartcommunitylab.aac.common.LoginException;
import it.smartcommunitylab.aac.config.ApplicationProperties;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.RealmManager;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityService;
import it.smartcommunitylab.aac.core.provider.LoginProvider;
import it.smartcommunitylab.aac.core.service.ClientDetailsService;
import it.smartcommunitylab.aac.core.service.IdentityProviderAuthorityService;
import it.smartcommunitylab.aac.core.service.IdentityServiceAuthorityService;
import it.smartcommunitylab.aac.model.Realm;

@Controller
@RequestMapping
public class LoginController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ApplicationProperties appProps;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private IdentityProviderAuthorityService identityProviderAuthorityService;

    @Autowired
    private IdentityServiceAuthorityService identityServiceAuthorityService;

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

        model.addAttribute("application", appProps);
        model.addAttribute("displayName", appProps.getName());
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

    // TODO split mapping in 2
    @RequestMapping(value = {
            "/-/{realm}/login",
            "/-/{realm}/login/{providerId}"
    }, method = RequestMethod.GET)
    public String login(
            @PathVariable("realm") String realm,
            @PathVariable("providerId") Optional<String> providerKey,
            @RequestParam(required = false, name = "client_id") Optional<String> clientKey,
            Model model, Locale locale,
            HttpServletRequest req, HttpServletResponse res) throws Exception {

        // TODO handle /login as COMMON login, ie any realm is valid
        String providerId = providerKey.isPresent() ? providerKey.get() : "";
        String clientId = clientKey.isPresent() ? clientKey.get() : null;

        if (!StringUtils.hasText(realm)) {
            throw new IllegalArgumentException("no suitable realm for login");
        }

        // load realm props
        model.addAttribute("realm", realm);
        model.addAttribute("displayName", realm);

        // fetch providers for given realm
        Collection<IdentityProvider<? extends UserIdentity, ?, ?, ?, ?>> providers = identityProviderAuthorityService
                .getAuthorities().stream()
                .flatMap(a -> a.getProvidersByRealm(realm).stream()).collect(Collectors.toList());

        if (StringUtils.hasText(providerId)) {
            Optional<IdentityProvider<? extends UserIdentity, ?, ?, ?, ?>> idp = providers.stream()
                    .filter(p -> p.getProvider().equals(providerId)).findFirst();
            if (idp.isPresent() && idp.get().getRealm().equals(realm)) {
                providers = Collections.singleton(idp.get());
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

        // fetch login providers
        // TODO refactor with proper provider + model
        List<LoginProvider> authorities = new ArrayList<>();
        for (IdentityProvider<? extends UserIdentity, ?, ?, ?, ?> idp : providers) {
            LoginProvider a = idp.getLoginProvider();
            // lp is optional
            if (a != null) {
                authorities.add(a);
            }
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

        // sort by position and name
        authorities.sort((LoginProvider l1, LoginProvider l2) -> {
            int c = l1.getPosition().compareTo(l2.getPosition());

            if (c == 0) {
                // use name
                c = l1.getName().compareTo(l2.getName());
            }

            return c;
        });

        // build a display list respecting display mode for ordering: form, button
        // TODO rework with comparable on model
        List<LoginProvider> loginAuthorities = new ArrayList<>();
        loginAuthorities.addAll(authorities.stream()
                .filter(a -> a.getTemplate().endsWith("form"))
                .collect(Collectors.toList()));
        loginAuthorities.addAll(authorities.stream()
                .filter(a -> "button".equals(a.getTemplate()))
                .collect(Collectors.toList()));

        model.addAttribute("authorities", loginAuthorities);

        // fetch account services for user registration
        Collection<IdentityService<? extends UserIdentity, ?, ?, ?, ?>> services = identityServiceAuthorityService
                .getAuthorities().stream()
                .flatMap(a -> a.getProvidersByRealm(realm).stream()).collect(Collectors.toList());

        // get registration entries
        // TODO replace with model, with ordering etc
        List<String> registrations = services.stream().map(s -> s.getRegistrationUrl()).filter(r -> r != null)
                .collect(Collectors.toList());
        model.addAttribute("registrations", registrations);

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

    private String logoEtagValue = null;

    @RequestMapping(value = "/logo", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<InputStreamResource> logo() throws IOException {

        // read resource as is
        Resource resource = resourceLoader.getResource(appProps.getLogo());
        if (resource == null) {
            throw new IOException();
        }

        // guess mimeType
        String contentType = "image/png";
        String fileName = resource.getFilename();
        if (fileName != null) {
            MimetypesFileTypeMap fileTypeMap = new MimetypesFileTypeMap();
            contentType = fileTypeMap.getContentType(fileName);
        }

        if (logoEtagValue == null) {
            // read fully and build etag once, this can change only on restart
            logoEtagValue = computeWeakEtag(resource.getInputStream());
        }

        return ResponseEntity.ok()
                .contentLength(resource.contentLength())
                .contentType(MediaType.parseMediaType(contentType))
                .cacheControl(CacheControl.maxAge(3600, TimeUnit.SECONDS))
                .eTag(logoEtagValue)
                .body(new InputStreamResource(resource.getInputStream()));

//        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }

    @RequestMapping(value = {
            "/-/{realm}/logo"
    }, method = RequestMethod.GET)
    public ResponseEntity<InputStreamResource> realmLogo() throws IOException {
        // TODO implement logo support per realm
        return logo();
    }

    private String computeWeakEtag(InputStream is) throws IOException {
        StringBuilder builder = new StringBuilder();
        // use same pattern as shallow etag filter
        builder.append("W/");
        builder.append("\"0");
        DigestUtils.appendMd5DigestAsHex(is, builder);
        builder.append('"');
        return builder.toString();
    }

}
