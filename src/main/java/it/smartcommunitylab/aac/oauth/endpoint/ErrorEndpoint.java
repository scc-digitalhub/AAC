package it.smartcommunitylab.aac.oauth.endpoint;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.HtmlUtils;

import io.swagger.v3.oas.annotations.Hidden;

@Hidden
@Controller
public class ErrorEndpoint {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String ERROR_URL = "/oauth/error";

    @Autowired
    private MessageSource messageSource;

    @RequestMapping(value = ERROR_URL)
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
