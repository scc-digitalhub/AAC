package it.smartcommunitylab.aac.oauth.endpoint;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.HtmlUtils;

import springfox.documentation.annotations.ApiIgnore;

@ApiIgnore
@Controller
public class ErrorEndpoint {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String ERROR_URL = "/oauth/error";

    @RequestMapping(value = ERROR_URL)
    public ModelAndView handleError(HttpServletRequest request) {
        // get error from request attribute
        Object error = request.getAttribute("error");
        String errorSummary;
        if (error instanceof OAuth2Exception) {
            OAuth2Exception oauthError = (OAuth2Exception) error;
            // error summary may contain user input, escape to avoid XSS
            errorSummary = HtmlUtils.htmlEscape(oauthError.getSummary());
        } else if (error instanceof String) {
            errorSummary = (String) error;
        } else {
            errorSummary = "Unknown error";
        }

        logger.debug("oauth2 error: " + errorSummary);

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("error", errorSummary);

        return new ModelAndView("oauth_error", model);
    }
}
