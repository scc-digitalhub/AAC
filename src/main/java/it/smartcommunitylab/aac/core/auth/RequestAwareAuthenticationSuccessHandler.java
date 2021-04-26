package it.smartcommunitylab.aac.core.auth;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.util.StringUtils;

/*
 * A global request aware success handler which supports both saved requests (from unauthorized) and internal 
 * requests for ToS, password reset, messages etc
 */
public class RequestAwareAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    public static final String SAVED_REQUEST = "AAC_SAVED_REQUEST";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session != null) {
            // first check our request
            String savedRequest = (String) session.getAttribute(SAVED_REQUEST);
            if (StringUtils.hasText(savedRequest)) {

                // clear authentication exceptions, we are already on success
                clearAuthenticationAttributes(request);

                // redirect
                getRedirectStrategy().sendRedirect(request, response, savedRequest);
                return;
            }
        }

        // let saved request proceed
        super.onAuthenticationSuccess(request, response, authentication);

    }

}
