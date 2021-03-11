package it.smartcommunitylab.aac.core.auth;

import java.util.Map;

import org.springframework.security.core.AuthenticatedPrincipal;

public interface UserAuthenticatedPrincipal extends AuthenticatedPrincipal {

    public String getAuthority();

    public String getRealm();

    public String getProvider();

    public String getUserId();

    public String getName();

    public Map<String, String> getAttributes();

//    public Map<String, String> getLinkingAttributes();

}
