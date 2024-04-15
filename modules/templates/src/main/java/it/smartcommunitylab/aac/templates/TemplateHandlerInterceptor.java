/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.templates;

import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchTemplateException;
import it.smartcommunitylab.aac.config.ApplicationProperties;
import it.smartcommunitylab.aac.core.entrypoint.RealmAwareUriBuilder;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.realms.service.RealmService;
import it.smartcommunitylab.aac.templates.model.FooterTemplate;
import it.smartcommunitylab.aac.templates.model.Template;
import it.smartcommunitylab.aac.templates.service.TemplateProviderAuthorityService;
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
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler,
        ModelAndView modelAndView
    ) throws Exception {
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
                // [authority]/[template]
                String[] s = viewName.split("/");
                if (s.length == 2) {
                    authority = s[0];
                    name = s[1];
                }

                logger.debug("fetch templates for {}:{} for realm {} with language {}", authority, name, realm, lang);

                Template template = null;
                Template footer = null;
                String customStyle = null;
                try {
                    // footer from base
                    footer = templateAuthority.getProviderByRealm(realm).getTemplate(FooterTemplate.TEMPLATE, locale);

                    // fetch template from authority
                    template = templateProviderAuthorityService
                        .getAuthority(authority)
                        .getProviderByRealm(realm)
                        .getTemplate(name, locale);

                    // fetch custom style from config
                    customStyle = templateAuthority.getProviderByRealm(realm).getConfig().getCustomStyle();
                } catch (NoSuchAuthorityException | NoSuchProviderException | NoSuchTemplateException e) {
                    // skip templates on error
                }

                modelAndView.addObject("template", template);
                modelAndView.addObject("footer", footer);
                modelAndView.addObject("customStyle", customStyle);
            }
        } catch (RuntimeException e) {
            // ignore errors to avoid stopping renderer
            logger.warn("error processing template: {}", e.getMessage());
        }
    }
}
