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

package it.smartcommunitylab.aac.oauth.endpoint;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.HtmlUtils;

@Hidden
@Controller
public class ErrorEndpoint {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String ERROR_URL = "/oauth/error";

    @Autowired
    private MessageSource messageSource;

    @RequestMapping(value = ERROR_URL, method = { RequestMethod.GET, RequestMethod.POST })
    public ModelAndView handleError(HttpServletRequest request) {
        // get error from request attribute
        Object error = request.getAttribute("error");
        String errorSummary;
        String errorClass = "Error";
        if (error instanceof OAuth2Exception) {
            OAuth2Exception oauthError = (OAuth2Exception) error;
            errorClass = oauthError.getOAuth2ErrorCode();
            // error summary may contain user input, escape to avoid XSS
            errorSummary = HtmlUtils.htmlEscape(oauthError.getSummary());
        } else if (error instanceof IllegalArgumentException) {
            IllegalArgumentException oauthError = (IllegalArgumentException) error;
            errorClass = "invalid_request";
            // error summary may contain user input, escape to avoid XSS
            errorSummary = HtmlUtils.htmlEscape(oauthError.getMessage());
        } else if (error instanceof String) {
            errorSummary = (String) error;
        } else {
            errorSummary = "Unknown error";
        }

        logger.debug("oauth2 error: " + errorSummary);

        // translate
        Locale locale = request.getLocale();
        String errorName = messageSource.getMessage("oauth2.error." + errorClass, null, errorClass, locale);

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("error", errorName);
        model.put("errorSummary", errorSummary);

        return new ModelAndView("oauth_error", model);
    }
}
