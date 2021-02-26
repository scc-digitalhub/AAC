package it.smartcommunitylab.aac.core.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.core.persistence.RoleEntity;
import it.smartcommunitylab.aac.core.persistence.RoleEntityRepository;
import it.smartcommunitylab.aac.model.Role;

@Service
public class RoleService {

    @Autowired
    private RoleEntityRepository roleRepository;

    public Set<Role> getRoles(String subject) {
        List<RoleEntity> rr = roleRepository.findBySubject(subject);

        return rr.stream()
                .map(r -> addRole(subject, r.getContext(), r.getSpace(), r.getRole()))
                .collect(Collectors.toSet());
    }

    public Set<Role> getRoles(String subject, String context) {
        List<RoleEntity> rr = roleRepository.findBySubjectAndContext(subject, context);

        return rr.stream()
                .map(r -> addRole(subject, r.getContext(), r.getSpace(), r.getRole()))
                .collect(Collectors.toSet());
    }

    public Set<Role> getRoles(String subject, String context, String space) {
        List<RoleEntity> rr = roleRepository.findBySubjectAndContextAndSpace(subject, context, space);

        return rr.stream()
                .map(r -> addRole(subject, r.getContext(), r.getSpace(), r.getRole()))
                .collect(Collectors.toSet());
    }

    public Role addRole(String subject, String context, String space, String role) {
        // check if exists
        RoleEntity r = roleRepository.findBySubjectAndContextAndSpaceAndRole(subject, context, space, role);
        if (r == null) {
            r = new RoleEntity();
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
        RoleEntity r = roleRepository.findBySubjectAndContextAndSpaceAndRole(subject, context, space, role);
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
        Set<RoleEntity> rr = new HashSet<>();
        for (Role role : roles) {
            RoleEntity r = roleRepository.findBySubjectAndContextAndSpaceAndRole(subject, role.getContext(),
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

        List<RoleEntity> rr = new ArrayList<>();

        // we sync attributes with those received by deleting missing
        List<RoleEntity> oldRoles = roleRepository.findBySubject(subject);

        List<RoleEntity> toRemove = new ArrayList<>();
        toRemove.addAll(oldRoles);

        for (Role role : roles) {
            RoleEntity r = roleRepository.findBySubjectAndContextAndSpaceAndRole(subject, role.getContext(),
                    role.getSpace(), role.getRole());

            if (r == null) {
                r = new RoleEntity();
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

    private static Role toRole(RoleEntity r) {
        // we keep this private to avoid exposing the builder
        return new Role(r.getContext(), r.getSpace(), r.getRole());
    }

}
