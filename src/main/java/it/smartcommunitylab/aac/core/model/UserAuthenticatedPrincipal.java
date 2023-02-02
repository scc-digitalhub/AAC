package it.smartcommunitylab.aac.core.model;

import java.io.Serializable;
import java.util.Map;

import org.springframework.security.core.AuthenticatedPrincipal;

/*
 * An authenticated user principal, associated with an identity provider and an account as a user resource
 * 
 * Every identity provider should implement a custom class exposing this interface for handling the authentication flow
 */
public interface UserAuthenticatedPrincipal extends AuthenticatedPrincipal, UserResource, Serializable {

    // principal name
    public String getUsername();

    // principal email
    public String getEmailAddress();

    public boolean isEmailVerified();

    // principal attributes as received from idp
    public Map<String, Serializable> getAttributes();

    // principalId is local to the provider
    String getPrincipalId();

}
