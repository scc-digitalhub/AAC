package it.smartcommunitylab.aac.core;

import java.util.Map;

import org.springframework.security.core.AuthenticatedPrincipal;

public interface UserAuthenticatedPrincipal extends AuthenticatedPrincipal {

    public String getUserId();

    public String getName();

    public Map<String, String> getAttributes();
}
