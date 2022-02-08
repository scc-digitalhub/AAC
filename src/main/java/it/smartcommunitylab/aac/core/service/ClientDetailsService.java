package it.smartcommunitylab.aac.core.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.persistence.ClientEntity;
import it.smartcommunitylab.aac.group.service.GroupService;
import it.smartcommunitylab.aac.model.Group;
import it.smartcommunitylab.aac.model.RealmRole;
import it.smartcommunitylab.aac.model.SpaceRole;
import it.smartcommunitylab.aac.roles.service.SpaceRoleService;
import it.smartcommunitylab.aac.roles.service.SubjectRoleService;

@Service
public class ClientDetailsService implements InitializingBean {

    // TODO add attributes service
    private final ClientEntityService clientService;
    private final SubjectService subjectService;

    private SpaceRoleService spaceRoleService;
    private SubjectRoleService subjectRoleService;
    private GroupService groupService;

    public ClientDetailsService(ClientEntityService clientService, SubjectService subjectService) {
        Assert.notNull(clientService, "client service is mandatory");
        Assert.notNull(subjectService, "subject service is mandatory");

        this.clientService = clientService;
        this.subjectService = subjectService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    @Autowired
    public void setSpaceRoleService(SpaceRoleService spaceRoleService) {
        this.spaceRoleService = spaceRoleService;
    }

    @Autowired
    public void setSubjectRoleService(SubjectRoleService subjectRoleService) {
        this.subjectRoleService = subjectRoleService;
    }

    @Autowired
    public void setGroupService(GroupService groupService) {
        this.groupService = groupService;
    }

    public ClientDetails loadClient(String clientId) throws NoSuchClientException {
        ClientEntity client = clientService.getClient(clientId);

        List<GrantedAuthority> clientAuthorities = subjectService.getAuthorities(clientId);

        // always set role_client
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority(Config.R_CLIENT));
        authorities.addAll(clientAuthorities);

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

        // TODO client attributes from attr providers?

        // load additional realm roles
        if (subjectRoleService != null) {
            // clientId is our subjectId
            Collection<RealmRole> clientRoles = subjectRoleService.getRoles(clientId);
            details.setRealmRoles(clientRoles);

        }

        // load space roles
        if (spaceRoleService != null) {
            // clientId is our subjectId
            Collection<SpaceRole> spaceRoles = spaceRoleService.getRoles(clientId);
            details.setSpaceRoles(spaceRoles);
        }

        // load groups
        if (groupService != null) {
            // clientId is our subjectId
            Collection<Group> groups = groupService.getSubjectGroups(clientId);
            details.setGroups(groups);
        }

        return details;

    }

}
