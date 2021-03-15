package it.smartcommunitylab.aac.core.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.core.persistence.ClientEntity;
import it.smartcommunitylab.aac.core.persistence.ClientEntityRepository;
import it.smartcommunitylab.aac.core.persistence.ClientRoleEntity;
import it.smartcommunitylab.aac.core.persistence.ClientRoleEntityRepository;

/*
 * Manage persistence for client entities and roles
 */
@Service
public class ClientEntityService {

    private final ClientEntityRepository clientRepository;
    private final ClientRoleEntityRepository clientRoleRepository;

    public ClientEntityService(ClientEntityRepository clientRepository,
            ClientRoleEntityRepository clientRoleRepository) {
        Assert.notNull(clientRepository, "client repository is mandatory");
        Assert.notNull(clientRoleRepository, "client roles repository is mandatory");
        this.clientRepository = clientRepository;
        this.clientRoleRepository = clientRoleRepository;
    }

    public ClientEntity createClient() {
        // generate random
        // TODO ensure unique on multi node deploy
        // (given that UUID is derived from timestamp we consider this safe enough)
        String uuid = UUID.randomUUID().toString();
        ClientEntity c = new ClientEntity(uuid);

        return c;
    }

    public ClientEntity addClient(
            String clientId, String realm,
            String type,
            String name, String description,
            Collection<String> scopes,
            Collection<String> providers) throws IllegalArgumentException {
        ClientEntity c = clientRepository.findByClientId(clientId);
        if (c != null) {
            throw new IllegalArgumentException("client already exists with the same id");
        }

        c = new ClientEntity(clientId);
        c.setRealm(realm);
        c.setType(type);
        c.setName(name);
        c.setDescription(description);

        c.setScopes(StringUtils.collectionToCommaDelimitedString(scopes));
        c.setProviders(StringUtils.collectionToCommaDelimitedString(providers));

        c = clientRepository.save(c);

        return c;

    }

    public ClientEntity findClient(String clientId) {
        return clientRepository.findByClientId(clientId);
    }

    public ClientEntity getClient(String clientId) throws NoSuchClientException {
        ClientEntity c = clientRepository.findByClientId(clientId);
        if (c == null) {
            throw new NoSuchClientException();
        }

        return c;
    }

    public ClientEntity updateClient(String clientId,
            String name, String description,
            Collection<String> scopes, Collection<String> providers) throws NoSuchClientException {
        ClientEntity c = clientRepository.findByClientId(clientId);
        if (c == null) {
            throw new NoSuchClientException();
        }

        c.setName(name);
        c.setDescription(description);

        c.setScopes(StringUtils.collectionToCommaDelimitedString(scopes));
        c.setProviders(StringUtils.collectionToCommaDelimitedString(providers));

        c = clientRepository.save(c);

        return c;

    }

    public void deleteClient(String clientId) {
        ClientEntity c = clientRepository.findByClientId(clientId);
        if (c != null) {
            // also search roles
            List<ClientRoleEntity> roles = clientRoleRepository.findByClientId(clientId);
            clientRoleRepository.deleteAll(roles);

            // remove entity
            clientRepository.delete(c);
        }
    }

    /*
     * Client roles
     */
    public List<ClientRoleEntity> getRoles(String clientId) throws NoSuchClientException {
        ClientEntity c = getClient(clientId);
        return clientRoleRepository.findByClientId(c.getClientId());
    }

    /*
     * Update client roles per realm
     */

    public List<ClientRoleEntity> updateRoles(String clientId, String realm, Collection<String> roles)
            throws NoSuchClientException {

        ClientEntity c = clientRepository.findByClientId(clientId);
        if (c == null) {
            throw new NoSuchClientException();
        }

        // fetch current roles
        List<ClientRoleEntity> oldRoles = clientRoleRepository.findByClientIdAndRealm(clientId, realm);

        // unpack roles
        Set<ClientRoleEntity> newRoles = roles.stream().map(r -> {
            ClientRoleEntity re = new ClientRoleEntity(clientId);
            re.setRealm(realm);
            re.setRole(r);
            return re;
        }).collect(Collectors.toSet());

        // update
        Set<ClientRoleEntity> toDelete = oldRoles.stream().filter(r -> !newRoles.contains(r))
                .collect(Collectors.toSet());
        Set<ClientRoleEntity> toAdd = newRoles.stream().filter(r -> !oldRoles.contains(r)).collect(Collectors.toSet());

        clientRoleRepository.deleteAll(toDelete);
        clientRoleRepository.saveAll(toAdd);

        return clientRoleRepository.findByClientIdAndRealm(clientId, realm);

    }

    /*
     * Update all client roles at once
     */
    public List<ClientRoleEntity> updateRoles(String clientId, Collection<Map.Entry<String, String>> roles)
            throws NoSuchClientException {

        ClientEntity c = clientRepository.findByClientId(clientId);
        if (c == null) {
            throw new NoSuchClientException();
        }

        // fetch current roles
        List<ClientRoleEntity> oldRoles = clientRoleRepository.findByClientId(clientId);

        // unpack roles
        Set<ClientRoleEntity> newRoles = roles.stream().map(e -> {
            ClientRoleEntity re = new ClientRoleEntity(clientId);
            re.setRealm(e.getKey());
            re.setRole(e.getValue());
            return re;
        }).collect(Collectors.toSet());

        // update
        Set<ClientRoleEntity> toDelete = oldRoles.stream().filter(r -> !newRoles.contains(r))
                .collect(Collectors.toSet());
        Set<ClientRoleEntity> toAdd = newRoles.stream().filter(r -> !oldRoles.contains(r)).collect(Collectors.toSet());

        clientRoleRepository.deleteAll(toDelete);
        clientRoleRepository.saveAll(toAdd);

        return clientRoleRepository.findByClientId(clientId);

    }

}
