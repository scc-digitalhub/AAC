package it.smartcommunitylab.aac.core.service;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.core.persistence.ClientEntity;
import it.smartcommunitylab.aac.core.persistence.ClientEntityRepository;
import it.smartcommunitylab.aac.model.Subject;

/*
 * Manage persistence for client entities and roles
 */
@Service
@Transactional
public class ClientEntityService {

    private final ClientEntityRepository clientRepository;

    // TODO move to clientService when implemented properly
    private final SubjectService subjectService;

    public ClientEntityService(ClientEntityRepository clientRepository,
            SubjectService subjectService) {
        Assert.notNull(clientRepository, "client repository is mandatory");
        Assert.notNull(subjectService, "subject service is mandatory");

        this.clientRepository = clientRepository;
        this.subjectService = subjectService;
    }

    public ClientEntity createClient() {
        String id = subjectService.generateUuid(SystemKeys.RESOURCE_CLIENT);
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

        // create subject
        Subject s = subjectService.addSubject(clientId, realm, SystemKeys.RESOURCE_CLIENT, name);

        // create client
        c = new ClientEntity(clientId);
        c.setRealm(realm);
        c.setType(type);
        c.setName(name);
        c.setDescription(description);

        c.setScopes(StringUtils.collectionToCommaDelimitedString(scopes));
        c.setResourceIds(StringUtils.collectionToCommaDelimitedString(resourceIds));
        c.setProviders(StringUtils.collectionToCommaDelimitedString(providers));

        // cleanup maps
        Map<String, String> hFunctions = hookFunctions == null ? null
                : hookFunctions.entrySet().stream()
                        .filter(e -> StringUtils.hasText(e.getValue()))
                        .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        Map<String, String> hWebUrls = hookWebUrls == null ? null
                : hookWebUrls.entrySet().stream()
                        .filter(e -> StringUtils.hasText(e.getValue()))
                        .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        c.setHookFunctions(hFunctions);
        c.setHookWebUrls(hWebUrls);
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

        // cleanup maps
        Map<String, String> hFunctions = hookFunctions == null ? null
                : hookFunctions.entrySet().stream()
                        .filter(e -> StringUtils.hasText(e.getValue()))
                        .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        Map<String, String> hWebUrls = hookWebUrls == null ? null
                : hookWebUrls.entrySet().stream()
                        .filter(e -> StringUtils.hasText(e.getValue()))
                        .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        c.setHookFunctions(hFunctions);
        c.setHookWebUrls(hWebUrls);
        c.setHookUniqueSpaces(hookUniqueSpaces);
        c = clientRepository.save(c);

        // check if subject exists and update name
        Subject s = subjectService.findSubject(clientId);
        if (s == null) {
            s = subjectService.addSubject(clientId, c.getRealm(), SystemKeys.RESOURCE_CLIENT, name);
        } else {
            try {
                s = subjectService.updateSubject(clientId, name);
            } catch (NoSuchSubjectException e) {
            }
        }

        return c;

    }

    public void deleteClient(String clientId) {
        ClientEntity c = clientRepository.findByClientId(clientId);
        if (c != null) {
            // remove entity
            clientRepository.delete(c);

            // remove subject if exists
            subjectService.deleteSubject(clientId);
        }
    }

//    /*
//     * Client authorities
//     */
//    @Transactional(readOnly = true)
//    public List<GrantedAuthority> getAuthorities(String clientId) throws NoSuchClientException {
//        ClientEntity c = getClient(clientId);
//        return clientRoleRepository.findByClientId(c.getClientId());
//    }
//
//    @Transactional(readOnly = true)
//    public List<ClientRoleEntity> getRoles(String clientId, String realm) throws NoSuchClientException {
//        ClientEntity c = getClient(clientId);
//        return clientRoleRepository.findByClientIdAndRealm(c.getClientId(), realm);
//    }
//
//    /*
//     * Update client roles per realm
//     */
//
//    public List<ClientRoleEntity> updateRoles(String clientId, String realm, Collection<String> roles)
//            throws NoSuchClientException {
//
//        ClientEntity c = clientRepository.findByClientId(clientId);
//        if (c == null) {
//            throw new NoSuchClientException();
//        }
//
//        // fetch current roles
//        List<ClientRoleEntity> oldRoles = clientRoleRepository.findByClientIdAndRealm(clientId, realm);
//
//        // unpack roles
//        Set<ClientRoleEntity> newRoles = roles.stream().map(r -> {
//            ClientRoleEntity re = new ClientRoleEntity(clientId);
//            re.setRealm(realm);
//            re.setRole(r);
//            return re;
//        }).collect(Collectors.toSet());
//
//        // update
//        Set<ClientRoleEntity> toDelete = oldRoles.stream().filter(r -> !newRoles.contains(r))
//                .collect(Collectors.toSet());
//        Set<ClientRoleEntity> toAdd = newRoles.stream().filter(r -> !oldRoles.contains(r)).collect(Collectors.toSet());
//
//        clientRoleRepository.deleteAll(toDelete);
//        clientRoleRepository.saveAll(toAdd);
//
//        return clientRoleRepository.findByClientIdAndRealm(clientId, realm);
//
//    }
//
//    /*
//     * Update all client roles at once
//     */
//    public List<ClientRoleEntity> updateRoles(String clientId, Collection<Map.Entry<String, String>> roles)
//            throws NoSuchClientException {
//
//        ClientEntity c = clientRepository.findByClientId(clientId);
//        if (c == null) {
//            throw new NoSuchClientException();
//        }
//
//        // fetch current roles
//        List<ClientRoleEntity> oldRoles = clientRoleRepository.findByClientId(clientId);
//
//        // unpack roles
//        Set<ClientRoleEntity> newRoles = roles.stream().map(e -> {
//            ClientRoleEntity re = new ClientRoleEntity(clientId);
//            re.setRealm(e.getKey());
//            re.setRole(e.getValue());
//            return re;
//        }).collect(Collectors.toSet());
//
//        // update
//        Set<ClientRoleEntity> toDelete = oldRoles.stream().filter(r -> !newRoles.contains(r))
//                .collect(Collectors.toSet());
//        Set<ClientRoleEntity> toAdd = newRoles.stream().filter(r -> !oldRoles.contains(r)).collect(Collectors.toSet());
//
//        clientRoleRepository.deleteAll(toDelete);
//        clientRoleRepository.saveAll(toAdd);
//
//        return clientRoleRepository.findByClientId(clientId);
//
//    }

}
