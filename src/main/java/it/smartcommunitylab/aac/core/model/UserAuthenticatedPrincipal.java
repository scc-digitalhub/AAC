package it.smartcommunitylab.aac.core.model;

import java.io.Serializable;
import java.util.Map;

import org.springframework.security.core.AuthenticatedPrincipal;

import it.smartcommunitylab.aac.SystemKeys;

/*
 * An authenticated user principal, associated with an identity provider and an account as a user resource
 * 
 * Every identity provider should implement a custom class exposing this interface for handling the authentication flow
 */
public interface UserAuthenticatedPrincipal extends AuthenticatedPrincipal, UserResource, Serializable {

    // principal name
    public String getName();

    // principal attributes as received from idp
    public Map<String, String> getAttributes();

    default String getType() {
        return SystemKeys.RESOURCE_PRINCIPAL;
    }

    // principalId is local to the provider
    default String getPrincipalId() {
        return getId();
    }

}
