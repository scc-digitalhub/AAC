package it.smartcommunitylab.aac.roles.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.model.SpaceRole;
import it.smartcommunitylab.aac.model.SpaceRoles;
import it.smartcommunitylab.aac.roles.persistence.SpaceRoleEntity;
import it.smartcommunitylab.aac.roles.persistence.SpaceRoleEntityRepository;

@Service
@Transactional
public class SpaceRoleService {

    @Autowired
    private SpaceRoleEntityRepository roleRepository;

    @Transactional(readOnly = true)
    public Collection<SpaceRole> getRoles(String subject) {
        List<SpaceRoleEntity> rr = roleRepository.findBySubject(subject);

        return rr.stream()
                .map(r -> addRole(subject, r.getContext(), r.getSpace(), r.getRole()))
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public Collection<SpaceRole> getRoles(String subject, String context) {
        List<SpaceRoleEntity> rr = roleRepository.findBySubjectAndContext(subject, context);

        return rr.stream()
                .map(r -> addRole(subject, r.getContext(), r.getSpace(), r.getRole()))
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public Page<SpaceRoles> getContextRoles(String incontext, String inspace, String q, Pageable pageRequest) {
        String context = StringUtils.hasText(incontext) ? incontext : null;
        String space = StringUtils.hasText(inspace) ? inspace : null;

        Page<String> rr = StringUtils.hasText(q)
                ? roleRepository.findByContextAndSpaceAndSubject(context, space, q.trim().toLowerCase(), pageRequest)
                : roleRepository.findByContextAndSpace(context, space, pageRequest);

        return PageableExecutionUtils.getPage(
                rr.getContent().stream().map(r -> {
                    Collection<SpaceRole> roles = getRoles(r, context, space);
                    SpaceRoles res = new SpaceRoles();
                    res.setContext(context);
                    res.setSpace(space);
                    res.setSubject(r);
                    res.setRoles(roles.stream().map(sr -> sr.getRole()).collect(Collectors.toList()));
                    return res;
                }).collect(Collectors.toList()),
                pageRequest,
                () -> rr.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Collection<SpaceRole> getRoles(String subject, String context, String space) {
        List<SpaceRoleEntity> rr = roleRepository.findBySubjectAndContextAndSpace(subject, context, space);

        return rr.stream()
                .map(r -> addRole(subject, r.getContext(), r.getSpace(), r.getRole()))
                .collect(Collectors.toSet());
    }

    public SpaceRole addRole(String subject, String context, String space, String role) {
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

    public Collection<SpaceRole> addRoles(String subject, Collection<SpaceRole> roles) {
        return roles.stream()
                .map(r -> addRole(subject, r.getContext(), r.getSpace(), r.getRole()))
                .collect(Collectors.toSet());
    }

    public void removeRoles(String subject, Collection<SpaceRole> roles) {
        // collect matching entitites
        Set<SpaceRoleEntity> rr = new HashSet<>();
        for (SpaceRole role : roles) {
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

    public Collection<SpaceRole> setRoles(String subject, Collection<SpaceRole> roles) {

        List<SpaceRoleEntity> rr = new ArrayList<>();

        // we sync attributes with those received by deleting missing
        List<SpaceRoleEntity> oldRoles = roleRepository.findBySubject(subject);

        List<SpaceRoleEntity> toRemove = new ArrayList<>();
        toRemove.addAll(oldRoles);

        for (SpaceRole role : roles) {
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

    public void deleteRoles(String subject) {
        List<SpaceRoleEntity> roles = roleRepository.findBySubject(subject);
        if (!roles.isEmpty()) {
            roleRepository.deleteAll(roles);
        }
    }

    /*
     * Helpers
     */

    private static SpaceRole toRole(SpaceRoleEntity r) {
        // we keep this private to avoid exposing the builder
        return new SpaceRole(r.getContext(), r.getSpace(), r.getRole());
    }

}
