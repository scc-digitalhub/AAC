package it.smartcommunitylab.aac.core.auth;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.web.authentication.AuthenticationConverter;

public interface ClientAuthenticationConverter extends AuthenticationConverter {

    @Override
    ClientAuthentication convert(HttpServletRequest request);
}
