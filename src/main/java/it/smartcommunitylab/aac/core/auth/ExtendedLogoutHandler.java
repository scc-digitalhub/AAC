package it.smartcommunitylab.aac.core.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

@Component
public class ExtendedLogoutHandler extends SecurityContextLogoutHandler {
    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        // let super process logout
        super.logout(request, response, authentication);

        // clear cookies
        // TODO
    }
}
