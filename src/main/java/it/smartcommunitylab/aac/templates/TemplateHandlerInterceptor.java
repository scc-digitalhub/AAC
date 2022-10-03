package it.smartcommunitylab.aac.templates;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchTemplateException;
import it.smartcommunitylab.aac.config.ApplicationProperties;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.core.model.Template;
import it.smartcommunitylab.aac.core.service.RealmService;
import it.smartcommunitylab.aac.core.service.TemplateProviderAuthorityService;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.templates.model.FooterTemplate;

@Component
public class TemplateHandlerInterceptor implements HandlerInterceptor {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ApplicationProperties appProps;

    @Autowired
    private RealmAwareUriBuilder uriBuilder;

    @Autowired
    private RealmService realmService;

    @Autowired
    private TemplateProviderAuthorityService templateProviderAuthorityService;

    @Autowired
    private TemplateAuthority templateAuthority;

    @Override
    public void postHandle(
            HttpServletRequest request, HttpServletResponse response,
            Object handler, ModelAndView modelAndView) throws Exception {
        if (modelAndView == null || !modelAndView.hasView()) {
            return;
        }

        String viewName = modelAndView.getViewName();
        if (viewName == null) {
            return;
        }

        if (viewName.startsWith("redirect:") || viewName.startsWith("forward:")) {
            return;
        }

        try {
            logger.debug("look for templates for view {}", viewName);

            // app props
            modelAndView.addObject("application", appProps);
            modelAndView.addObject("appName", appProps.getName());

            // realm
            Object slug = modelAndView.getModel().get("realm");
            if (slug != null && slug instanceof String) {
                String realm = (String) slug;
                Locale locale = LocaleContextHolder.getLocale();
                String lang = locale.getLanguage();

                // load realm props
                Realm r = realmService.getRealm(realm);

                ApplicationProperties props = new ApplicationProperties();
                props.setName(r.getName());

                // build props from global
                // TODO add fields to realm config
                props.setEmail(appProps.getEmail());
                props.setLang(appProps.getLang());
                props.setLogo(appProps.getLogo());

                // via urlBuilder
                String url = uriBuilder.buildUrl(realm, "/");
                props.setUrl(url);

                String displayName = r.getName();

                modelAndView.addObject("displayName", displayName);
                modelAndView.addObject("props", props);

                // fetch templates
                String authority = templateAuthority.getAuthorityId();
                String name = viewName;

                // extract template name by conventions
                // [template]
                // [template]_variants
                // [authority]/[template]_[variants]
                String[] s = viewName.split("/");
                if (s.length == 2) {
                    authority = s[0];
                    name = s[1];
                }

                if (name.contains("_")) {
                    s = name.split("_");
                    name = s[0];
                }

                logger.debug("fetch templates for {}:{} for realm {} with language {}", authority, name, realm, lang);

                Template template = null;
                Template footer = null;
                try {
                    // footer from base
                    footer = templateAuthority
                            .getProviderByRealm(realm)
                            .getTemplate(FooterTemplate.TEMPLATE, locale);

                    // fetch template from authority
                    template = templateProviderAuthorityService.getAuthority(authority)
                            .getProviderByRealm(realm)
                            .getTemplate(name, locale);

                } catch (NoSuchAuthorityException | NoSuchProviderException | NoSuchTemplateException e) {
                    // skip templates on error
                }

                modelAndView.addObject("template", template);
                modelAndView.addObject("footer", footer);
            }
        } catch (RuntimeException e) {
            // ignore errors to avoid stopping renderer
            logger.warn("error processing template: {}", e.getMessage());
        }
    }

}
