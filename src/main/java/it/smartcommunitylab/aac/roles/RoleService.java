package it.smartcommunitylab.aac.roles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.model.Role;
import it.smartcommunitylab.aac.roles.persistence.SpaceRoleEntity;
import it.smartcommunitylab.aac.roles.persistence.SpaceRoleEntityRepository;

@Service
public class RoleService {

    @Autowired
    private SpaceRoleEntityRepository roleRepository;

    public Set<Role> getRoles(String subject) {
        List<SpaceRoleEntity> rr = roleRepository.findBySubject(subject);

        return rr.stream()
                .map(r -> addRole(subject, r.getContext(), r.getSpace(), r.getRole()))
                .collect(Collectors.toSet());
    }

    public Set<Role> getRoles(String subject, String context) {
        List<SpaceRoleEntity> rr = roleRepository.findBySubjectAndContext(subject, context);

        return rr.stream()
                .map(r -> addRole(subject, r.getContext(), r.getSpace(), r.getRole()))
                .collect(Collectors.toSet());
    }

    public Set<Role> getRoles(String subject, String context, String space) {
        List<SpaceRoleEntity> rr = roleRepository.findBySubjectAndContextAndSpace(subject, context, space);

        return rr.stream()
                .map(r -> addRole(subject, r.getContext(), r.getSpace(), r.getRole()))
                .collect(Collectors.toSet());
    }

    public Role addRole(String subject, String context, String space, String role) {
        // check if exists
        SpaceRoleEntity r = roleRepository.findBySubjectAndContextAndSpaceAndRole(subject, context, space, role);
        if (r == null) {
            r = new SpaceRoleEntity();
            r.setSubject(subject);
            r.setContext(context);
            r.setSpace(space);
            r.setRole(role);

            r = roleRepository.save(r);
        }

        return toRole(r);
    }

    public void removeRole(String subject, String context, String space, String role) {
        // check if exists
        SpaceRoleEntity r = roleRepository.findBySubjectAndContextAndSpaceAndRole(subject, context, space, role);
        if (r != null) {
            roleRepository.delete(r);
        }
    }

    public Set<Role> addRoles(String subject, Collection<Role> roles) {
        return roles.stream()
                .map(r -> addRole(subject, r.getContext(), r.getSpace(), r.getRole()))
                .collect(Collectors.toSet());
    }

    public void removeRoles(String subject, Collection<Role> roles) {
        // collect matching entitites
        Set<SpaceRoleEntity> rr = new HashSet<>();
        for (Role role : roles) {
            SpaceRoleEntity r = roleRepository.findBySubjectAndContextAndSpaceAndRole(subject, role.getContext(),
                    role.getSpace(), role.getRole());
            if (r != null) {
                rr.add(r);
            }
        }

        if (!rr.isEmpty()) {
            roleRepository.deleteAll(rr);
        }
    }

    public Set<Role> setRoles(String subject, Collection<Role> roles) {

        List<SpaceRoleEntity> rr = new ArrayList<>();

        // we sync attributes with those received by deleting missing
        List<SpaceRoleEntity> oldRoles = roleRepository.findBySubject(subject);

        List<SpaceRoleEntity> toRemove = new ArrayList<>();
        toRemove.addAll(oldRoles);

        for (Role role : roles) {
            SpaceRoleEntity r = roleRepository.findBySubjectAndContextAndSpaceAndRole(subject, role.getContext(),
                    role.getSpace(), role.getRole());

            if (r == null) {
                r = new SpaceRoleEntity();
                r.setSubject(subject);
                r.setContext(role.getContext());
                r.setSpace(role.getSpace());
                r.setRole(role.getRole());

                r = roleRepository.save(r);
            }

            if (toRemove.contains(r)) {
                toRemove.remove(r);
            }

            rr.add(r);
        }

        // remove orphans
        roleRepository.deleteAll(toRemove);

        return rr.stream().map(r -> toRole(r)).collect(Collectors.toSet());

    }

    /*
     * Helpers
     */

    private static Role toRole(SpaceRoleEntity r) {
        // we keep this private to avoid exposing the builder
        return new Role(r.getContext(), r.getSpace(), r.getRole());
    }

}
