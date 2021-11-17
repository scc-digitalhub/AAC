package it.smartcommunitylab.aac.saml.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationRequestContext;

public interface Saml2AuthenticationRequestRepository<T extends Saml2AuthenticationRequestContext> {

    T loadAuthenticationRequest(HttpServletRequest request);

    void saveAuthenticationRequest(T authenticationRequest, HttpServletRequest request, HttpServletResponse response);

    T removeAuthenticationRequest(HttpServletRequest request, HttpServletResponse response);
}
