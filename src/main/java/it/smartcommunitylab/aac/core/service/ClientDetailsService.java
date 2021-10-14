package it.smartcommunitylab.aac.core.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.auth.RealmGrantedAuthority;
import it.smartcommunitylab.aac.core.persistence.ClientEntity;
import it.smartcommunitylab.aac.core.persistence.ClientRoleEntity;
import it.smartcommunitylab.aac.roles.RoleService;

@Service
public class ClientDetailsService {

    // TODO add attributes service
    private final ClientEntityService clientService;

    private RoleService roleService;

    public ClientDetailsService(ClientEntityService clientService) {
        Assert.notNull(clientService, "client service is mandatoy");
        this.clientService = clientService;
    }

    @Autowired
    public void setRoleService(RoleService roleService) {
        this.roleService = roleService;
    }

    public ClientDetails loadClient(String clientId) throws NoSuchClientException {
        ClientEntity client = clientService.getClient(clientId);

        // always set role_client
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority(Config.R_CLIENT));

        // load additional realm roles
        List<ClientRoleEntity> clientRoles = clientService.getRoles(clientId);

        authorities.addAll(clientRoles.stream()
                .filter(r -> !StringUtils.hasText(r.getRealm()))
                .map(r -> new SimpleGrantedAuthority(r.getRole()))
                .collect(Collectors.toSet()));

        authorities.addAll(clientRoles.stream()
                .filter(r -> StringUtils.hasText(r.getRealm()))
                .map(r -> new RealmGrantedAuthority(r.getRealm(), r.getRole()))
                .collect(Collectors.toSet()));

        ClientDetails details = new ClientDetails(
                client.getClientId(), client.getRealm(),
                client.getType(),
                authorities);

        details.setName(client.getName());
        details.setDescription(client.getDescription());
        details.setProviders(StringUtils.commaDelimitedListToSet(client.getProviders()));
        details.setScopes(StringUtils.commaDelimitedListToSet(client.getScopes()));
        details.setResourceIds(StringUtils.commaDelimitedListToSet(client.getResourceIds()));
        details.setHookFunctions(client.getHookFunctions());
        details.setHookWebUrls(client.getHookWebUrls());
        details.setHookUniqueSpaces(client.getHookUniqueSpaces());
        // TODO client attributes from attr providers

        // load space roles
        if (roleService != null) {
            // clientId is our subjectId
            details.setRoles(roleService.getRoles(clientId));
        }

        return details;

    }

}
