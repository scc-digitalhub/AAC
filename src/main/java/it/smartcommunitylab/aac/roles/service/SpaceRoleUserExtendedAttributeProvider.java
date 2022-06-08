package it.smartcommunitylab.aac.roles.service;

import java.io.Serializable;
import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.aac.core.provider.UserExtendedAttributeProvider;
import it.smartcommunitylab.aac.model.User;

@Component
public class SpaceRoleUserExtendedAttributeProvider implements UserExtendedAttributeProvider {

    public static final String KEY = "SPACE_ROLES";

    @Autowired
    private SpaceRoleService spaceRoleService;

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public Collection<Serializable> fetchExtendedAttributes(User user, String realm) {
        return spaceRoleService.getRoles(user.getUserId()).stream().map(r -> (Serializable) r)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteExtendedAttributes(String userId, String realm) {
        spaceRoleService.deleteRoles(userId);
    }

}
