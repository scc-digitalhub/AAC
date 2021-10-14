package it.smartcommunitylab.aac.core.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.core.persistence.ClientEntity;
import it.smartcommunitylab.aac.core.persistence.ClientEntityRepository;
import it.smartcommunitylab.aac.core.persistence.ClientRoleEntity;
import it.smartcommunitylab.aac.core.persistence.ClientRoleEntityRepository;
import it.smartcommunitylab.aac.core.persistence.SubjectEntity;
import it.smartcommunitylab.aac.core.persistence.SubjectEntityRepository;

/*
 * Manage persistence for client entities and roles
 */
@Service
@Transactional
public class ClientEntityService {

    private final ClientEntityRepository clientRepository;
    private final ClientRoleEntityRepository clientRoleRepository;
    private final SubjectEntityRepository subjectRepository;

    public ClientEntityService(ClientEntityRepository clientRepository,
            ClientRoleEntityRepository clientRoleRepository,
            SubjectEntityRepository subjectRepository) {
        Assert.notNull(clientRepository, "client repository is mandatory");
        Assert.notNull(clientRoleRepository, "client roles repository is mandatory");
        Assert.notNull(subjectRepository, "subject repository is mandatory");

        this.clientRepository = clientRepository;
        this.clientRoleRepository = clientRoleRepository;
        this.subjectRepository = subjectRepository;
    }

    public ClientEntity createClient() {
        // generate random
        // TODO ensure unique on multi node deploy: replace with idGenerator
        // (given that UUID is derived from timestamp we consider this safe enough)
        String uuid = UUID.randomUUID().toString();

        // we prepend a fixed prefix to enable discovery of entity type from uuid
        String id = ClientEntity.ID_PREFIX + uuid;

        ClientEntity c = new ClientEntity(id);

        return c;
    }

    public ClientEntity addClient(
            String clientId, String realm,
            String type,
            String name, String description,
            Collection<String> scopes, Collection<String> resourceIds,
            Collection<String> providers,
            Map<String, String> hookFunctions,
            Map<String, String> hookWebUrls,
            String hookUniqueSpaces) throws IllegalArgumentException {

        ClientEntity c = clientRepository.findByClientId(clientId);
        if (c != null) {
            throw new IllegalArgumentException("client already exists with the same id");
        }

        // create a subject, will throw error if exists
        SubjectEntity s = new SubjectEntity(clientId);
        s.setRealm(realm);
        s.setType(SystemKeys.RESOURCE_CLIENT);
        s.setName(name);
        s = subjectRepository.save(s);

        // create client
        c = new ClientEntity(clientId);
        c.setRealm(realm);
        c.setType(type);
        c.setName(name);
        c.setDescription(description);

        c.setScopes(StringUtils.collectionToCommaDelimitedString(scopes));
        c.setResourceIds(StringUtils.collectionToCommaDelimitedString(resourceIds));
        c.setProviders(StringUtils.collectionToCommaDelimitedString(providers));

        c.setHookFunctions(hookFunctions);
        c.setHookWebUrls(hookWebUrls);
        c.setHookUniqueSpaces(hookUniqueSpaces);

        c = clientRepository.save(c);
        return c;

    }

    @Transactional(readOnly = true)
    public ClientEntity findClient(String clientId) {
        return clientRepository.findByClientId(clientId);
    }

    @Transactional(readOnly = true)
    public ClientEntity getClient(String clientId) throws NoSuchClientException {
        ClientEntity c = clientRepository.findByClientId(clientId);
        if (c == null) {
            throw new NoSuchClientException("no client for id " + String.valueOf(clientId));
        }

        return c;
    }

    @Transactional(readOnly = true)
    public long countClients(String realm) {
        return clientRepository.countByRealm(realm);
    }

    @Transactional(readOnly = true)
    public Collection<ClientEntity> listClients() {
        return clientRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Collection<ClientEntity> listClients(String realm) {
        return clientRepository.findByRealm(realm);
    }

    @Transactional(readOnly = true)
    public Collection<ClientEntity> findClientsByName(String realm, String name) {
        return clientRepository.findByRealmAndName(realm, name);
    }

    @Transactional(readOnly = true)
    public Collection<ClientEntity> findClientsByType(String realm, String type) {
        return clientRepository.findByRealmAndType(realm, type);
    }

    @Transactional(readOnly = true)
    public Page<ClientEntity> searchClients(String realm, String q, Pageable pageRequest) {
        Page<ClientEntity> page = StringUtils.hasText(q)
                ? clientRepository.findByRealmAndNameContainingIgnoreCaseOrRealmAndClientIdContainingIgnoreCase(realm,
                        q, realm,
                        q, pageRequest)
                : clientRepository.findByRealm(realm, pageRequest);

        return page;
    }

    public ClientEntity updateClient(String clientId,
            String name, String description,
            Collection<String> scopes, Collection<String> resourceIds,
            Collection<String> providers,
            Map<String, String> hookFunctions,
            Map<String, String> hookWebUrls,
            String hookUniqueSpaces) throws NoSuchClientException {
        ClientEntity c = clientRepository.findByClientId(clientId);
        if (c == null) {
            throw new NoSuchClientException();
        }

        c.setName(name);
        c.setDescription(description);

        c.setScopes(StringUtils.collectionToCommaDelimitedString(scopes));
        c.setResourceIds(StringUtils.collectionToCommaDelimitedString(resourceIds));
        c.setProviders(StringUtils.collectionToCommaDelimitedString(providers));

        c.setHookFunctions(hookFunctions);
        c.setHookWebUrls(hookWebUrls);
        c.setHookUniqueSpaces(hookUniqueSpaces);
        c = clientRepository.save(c);

        // check if subject exists and update name
        SubjectEntity s = subjectRepository.findBySubjectId(clientId);
        if (s == null) {
            s = new SubjectEntity(clientId);
            s.setRealm(c.getRealm());
            s.setType(SystemKeys.RESOURCE_CLIENT);
        }
        s.setName(name);
        s = subjectRepository.save(s);

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

            // remove subject if exists
            if (subjectRepository.findOne(clientId) != null) {
                subjectRepository.deleteById(clientId);
            }
        }
    }

    /*
     * Client roles
     */
    @Transactional(readOnly = true)
    public List<ClientRoleEntity> getRoles(String clientId) throws NoSuchClientException {
        ClientEntity c = getClient(clientId);
        return clientRoleRepository.findByClientId(c.getClientId());
    }

    @Transactional(readOnly = true)
    public List<ClientRoleEntity> getRoles(String clientId, String realm) throws NoSuchClientException {
        ClientEntity c = getClient(clientId);
        return clientRoleRepository.findByClientIdAndRealm(c.getClientId(), realm);
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
