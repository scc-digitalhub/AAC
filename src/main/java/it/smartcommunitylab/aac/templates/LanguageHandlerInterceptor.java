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

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.internal.templates.InternalRegisterAccountTemplate;
import it.smartcommunitylab.aac.templates.model.LoginTemplate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class LanguageHandlerInterceptor implements HandlerInterceptor {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final Set<String> WHITELIST_VIEWS;
    private static final String[] WHITELIST_VIEWS_VALUES = {
        LoginTemplate.TEMPLATE,
        SystemKeys.AUTHORITY_INTERNAL + "/" + InternalRegisterAccountTemplate.TEMPLATE,
    };

    static {
        WHITELIST_VIEWS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(WHITELIST_VIEWS_VALUES)));
    }

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

        // white-list only GET requests
        if (!request.getMethod().equals(RequestMethod.GET.name())) {
            return;
        }

        // white-list only specific views
        if (!WHITELIST_VIEWS.contains(viewName)) {
            return;
        }

        try {
            logger.debug("load languages for view {}", viewName);

            // realm
            Object slug = modelAndView.getModel().get("realm");
            if (slug != null && slug instanceof String) {
                String realm = (String) slug;
                Locale locale = LocaleContextHolder.getLocale();
                String language = locale.getLanguage();

                logger.debug("current language is {}", language);
                modelAndView.addObject("language", language);

                UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(request.getRequestURL().toString());

                List<LanguageValue> languages = new ArrayList<>();
                try {
                    // load realm languages
                    templateAuthority
                        .getProviderByRealm(realm)
                        .getConfig()
                        .getLanguages()
                        .forEach(l -> {
                            Locale loc = StringUtils.parseLocale(l);
                            if (loc == null) {
                                return;
                            }

                            // build url with param
                            Map<String,String[]> parameterMap = new HashMap<>(request.getParameterMap());
                            parameterMap.remove("lang");

                            MultiValueMap<String, String> parameterMVMap = new LinkedMultiValueMap<>();
                            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                                parameterMVMap.put(entry.getKey(), Arrays.asList(entry.getValue()));
                            }

                            builder.replaceQueryParams(parameterMVMap);
                            builder.queryParam("lang", l);
                            String url = builder.build().toString();
                            String label = loc.getDisplayLanguage(locale);
                            LanguageValue lv = new LanguageValue();
                            lv.setCode(l);
                            lv.setLabel(label);
                            lv.setUrl(url);

                            languages.add(lv);
                        });
                } catch (NoSuchProviderException e) {
                    // skip on error
                }

                modelAndView.addObject("languages", languages);

                if (logger.isTraceEnabled()) {
                    logger.trace("available languages {}", languages);
                }
            }
        } catch (RuntimeException e) {
            // ignore errors to avoid stopping renderer
            logger.warn("error processing languages: {}", e.getMessage());
        }
    }

    static class LanguageValue {

        private String code;
        private String label;
        private String url;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        @Override
        public String toString() {
            return "LanguageValue [code=" + code + "]";
        }
    }
}
