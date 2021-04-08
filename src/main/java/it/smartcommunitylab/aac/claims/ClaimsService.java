package it.smartcommunitylab.aac.claims;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchResourceException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.model.User;

public interface ClaimsService {

    /*
     * Complete mapping TODO move to dedicated interface claimMapper
     */
    public Map<String, Serializable> getUserClaims(UserDetails user, String realm, ClientDetails client,
            Collection<String> scopes,
            Collection<String> resourceIds) throws NoSuchResourceException, InvalidDefinitionException, SystemException;

    public Map<String, Serializable> getUserClaims(User user, ClientDetails client, Collection<String> scopes,
            Collection<String> resourceIds) throws NoSuchResourceException, InvalidDefinitionException, SystemException;

    public Map<String, Serializable> getClientClaims(ClientDetails client, Collection<String> scopes,
            Collection<String> resourceIds) throws NoSuchResourceException, InvalidDefinitionException, SystemException;

//    /*
//     * ProfileMapping
//     */
//
//    public Map<String, Serializable> getUserClaimsFromBasicProfile(UserDetails user);
//
//    public Map<String, Serializable> getUserClaimsFromOpenIdProfile(UserDetails user, Collection<String> scopes);
//
//    /*
//     * Service mapping
//     */
//    public Map<String, Serializable> getUserClaimsFromResource(UserDetails user, ClientDetails client,
//            Collection<String> scopes,
//            String resourceId) throws NoSuchResourceException;
//
//    public Map<String, Serializable> getClientClaimsFromResource(ClientDetails client, Collection<String> scopes,
//            String resourceId) throws NoSuchResourceException;

//    /*
//     * Function mapping for hooks
//     */
//    public Map<String, Serializable> mapUserClaimsWithFunction(UserDetails user, ClientDetails client,
//            Collection<String> scopes,
//            String functionName, String functionCode) throws InvalidDefinitionException;
//
//    public Map<String, Serializable> mapClientClaimsFromFunction(ClientDetails client, Collection<String> scopes,
//            String functionName, String functionCode) throws InvalidDefinitionException;

}
