package it.smartcommunitylab.aac.core.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;

public interface LoginUrlRequestConverter {

    String convert(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException);

}
