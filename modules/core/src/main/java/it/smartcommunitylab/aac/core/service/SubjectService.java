/**
 * Copyright 2024 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.core.service;

import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.core.persistence.SubjectAuthorityEntity;
import it.smartcommunitylab.aac.core.persistence.SubjectAuthorityEntityRepository;
import it.smartcommunitylab.aac.core.provider.SubjectProvider;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.repository.PaginationUtils.PageOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Service
@Slf4j
@Transactional
public class SubjectService {

    private final SubjectAuthorityEntityRepository authorityRepository;
    private TreeMap<String, SubjectProvider<? extends Subject>> providers;

    public SubjectService(
        SubjectAuthorityEntityRepository authorityRepository,
        List<SubjectProvider<? extends Subject>> providers
    ) {
        Assert.notNull(authorityRepository, "autorities repository is required");
        Assert.notNull(providers, "providers can not be null");

        this.authorityRepository = authorityRepository;

        //collect into key-ordered map
        this.providers = providers
            .stream()
            .collect(Collectors.toMap(p -> p.getClass().getSimpleName(), p -> p, (a, b) -> a, TreeMap::new));

        log.debug("initialized with providers for {}", this.providers.keySet());
    }

    @Transactional(readOnly = true)
    public Subject getSubject(@NotNull String id) throws NoSuchSubjectException {
        log.debug("get subject with id {}", String.valueOf(id));
        Subject s = providers.values().stream().map(p -> p.findSubject(id)).findFirst().orElse(null);
        if (s == null) {
            throw new NoSuchSubjectException();
        }

        if (log.isTraceEnabled()) {
            log.trace("sub: {}", s);
        }

        return s;
    }

    @Transactional(readOnly = true)
    public Subject findSubject(@NotNull String id) {
        log.debug("find subject with id {}", String.valueOf(id));

        //search via providers
        return providers.values().stream().map(p -> p.findSubject(id)).findFirst().orElse(null);
    }

    @Transactional(readOnly = true)
    public Page<Subject> listSubjects(@NotNull String realm, Pageable page) {
        log.debug("list subjects for realm {} - page {}", String.valueOf(realm), String.valueOf(page));

        //manage global pagination via offset + order
        Map<SubjectProvider<? extends Subject>, Long> totals = providers
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    e -> e.getValue(),
                    e -> e.getValue().listSubjectsByRealm(realm, Pageable.ofSize(1)).getTotalElements()
                )
            );

        long total = totals.values().stream().collect(Collectors.summingLong(Long::longValue));

        List<Subject> list = new ArrayList<>();
        long offset = 0;
        for (Map.Entry<SubjectProvider<? extends Subject>, Long> e : totals.entrySet()) {
            if (list.size() >= page.getPageSize()) {
                //page is full
                break;
            }

            //missing elements
            int m = page.getPageSize() - list.size();

            //local total
            long t = e.getValue().longValue();
            long l = offset + t;

            //build page size
            //collect all available in a single page
            int s = t <= Integer.MAX_VALUE ? (int) t : Integer.MAX_VALUE;
            if (m <= l) {
                //we can collect all missing from a single page
                s = m;
            }

            //build local offset
            long o = 0;

            if (page.getOffset() <= offset) {
                //start exactly from here
                o = 0;
            } else {
                o = page.getOffset() - offset;
                if (o > t) {
                    //exceeded, skip
                    continue;
                }
            }

            //collect as page and accumulate
            list.addAll(e.getKey().listSubjectsByRealm(realm, new PageOffset(o, s, page.getSort())).getContent());
        }

        return new PageImpl<>(list, page, total);
    }

    @Transactional(readOnly = true)
    public Page<Subject> searchSubjects(@NotNull String realm, @NotNull String q, Pageable page) {
        log.debug(
            "search subjects for realm {} via q: {} - page {}",
            String.valueOf(realm),
            String.valueOf(q),
            String.valueOf(page)
        );

        //TODO
        // return StringUtils.hasText(q)
        //     ? providers
        //         .values()
        //         .stream()
        //         .flatMap(p -> p.searchSubjectsByRealm(realm, q).stream())
        //         .collect(Collectors.toList())
        //     : Page.empty();

        return Page.empty();
    }

    /*
     * Authorities
     */

    @Transactional(readOnly = true)
    public List<GrantedAuthority> getAuthorities(String subjectId) {
        return authorityRepository
            .findBySubject(subjectId)
            .stream()
            .map(a -> toAuthority(a))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GrantedAuthority> getAuthorities(String subjectId, String realm) {
        return authorityRepository
            .findBySubjectAndRealm(subjectId, realm)
            .stream()
            .map(a -> toAuthority(a))
            .collect(Collectors.toList());
    }

    public List<GrantedAuthority> addAuthorities(String uuid, String realm, Collection<String> roles)
        throws NoSuchSubjectException {
        //fetch subject
        Subject s = findSubject(uuid);
        if (s == null) {
            throw new NoSuchSubjectException("subject not found for " + uuid);
        }

        // fetch current roles
        List<SubjectAuthorityEntity> oldRoles = authorityRepository.findBySubjectAndRealm(uuid, realm);

        // unpack roles
        Set<SubjectAuthorityEntity> newRoles = roles
            .stream()
            .map(r -> {
                SubjectAuthorityEntity re = new SubjectAuthorityEntity(uuid);
                re.setRealm(realm);
                re.setRole(r);
                return re;
            })
            .collect(Collectors.toSet());

        // update
        Set<SubjectAuthorityEntity> toAdd = newRoles
            .stream()
            .filter(r -> !oldRoles.contains(r))
            .collect(Collectors.toSet());

        return authorityRepository.saveAll(toAdd).stream().map(a -> toAuthority(a)).collect(Collectors.toList());
    }

    public List<GrantedAuthority> addAuthorities(String uuid, Collection<Map.Entry<String, String>> roles)
        throws NoSuchSubjectException {
        //fetch subject
        Subject s = findSubject(uuid);
        if (s == null) {
            throw new NoSuchSubjectException("subject not found for " + uuid);
        }

        // fetch current roles
        List<SubjectAuthorityEntity> oldRoles = authorityRepository.findBySubject(uuid);

        // unpack roles
        Set<SubjectAuthorityEntity> newRoles = roles
            .stream()
            .map(e -> {
                SubjectAuthorityEntity re = new SubjectAuthorityEntity(uuid);
                re.setRealm(e.getKey());
                re.setRole(e.getValue());
                return re;
            })
            .collect(Collectors.toSet());

        // update
        Set<SubjectAuthorityEntity> toAdd = newRoles
            .stream()
            .filter(r -> !oldRoles.contains(r))
            .collect(Collectors.toSet());

        return authorityRepository.saveAll(toAdd).stream().map(a -> toAuthority(a)).collect(Collectors.toList());
    }

    public void removeAuthorities(String uuid, String realm, Collection<String> roles) throws NoSuchSubjectException {
        //fetch subject
        Subject s = findSubject(uuid);
        if (s == null) {
            throw new NoSuchSubjectException("subject not found for " + uuid);
        }

        // fetch current roles
        List<SubjectAuthorityEntity> oldRoles = authorityRepository.findBySubjectAndRealm(uuid, realm);

        // unpack roles
        Set<SubjectAuthorityEntity> newRoles = roles
            .stream()
            .map(r -> {
                SubjectAuthorityEntity re = new SubjectAuthorityEntity(uuid);
                re.setRealm(realm);
                re.setRole(r);
                return re;
            })
            .collect(Collectors.toSet());

        // update
        Set<SubjectAuthorityEntity> toDelete = oldRoles
            .stream()
            .filter(r -> newRoles.contains(r))
            .collect(Collectors.toSet());

        authorityRepository.deleteAll(toDelete);
    }

    public void removeAuthorities(String uuid, Collection<Map.Entry<String, String>> roles)
        throws NoSuchSubjectException {
        //fetch subject
        Subject s = findSubject(uuid);
        if (s == null) {
            throw new NoSuchSubjectException("subject not found for " + uuid);
        }

        // fetch current roles
        List<SubjectAuthorityEntity> oldRoles = authorityRepository.findBySubject(uuid);

        // unpack roles
        Set<SubjectAuthorityEntity> newRoles = roles
            .stream()
            .map(e -> {
                SubjectAuthorityEntity re = new SubjectAuthorityEntity(uuid);
                re.setRealm(e.getKey());
                re.setRole(e.getValue());
                return re;
            })
            .collect(Collectors.toSet());

        // update
        Set<SubjectAuthorityEntity> toDelete = oldRoles
            .stream()
            .filter(r -> newRoles.contains(r))
            .collect(Collectors.toSet());

        authorityRepository.deleteAll(toDelete);
    }

    public List<GrantedAuthority> updateAuthorities(String uuid, String realm, Collection<String> roles)
        throws NoSuchSubjectException {
        //fetch subject
        Subject s = findSubject(uuid);
        if (s == null) {
            throw new NoSuchSubjectException("subject not found for " + uuid);
        }

        // fetch current roles
        List<SubjectAuthorityEntity> oldRoles = authorityRepository.findBySubjectAndRealm(uuid, realm);

        // unpack roles
        Set<SubjectAuthorityEntity> newRoles = roles
            .stream()
            .map(r -> {
                SubjectAuthorityEntity re = new SubjectAuthorityEntity(uuid);
                re.setRealm(realm);
                re.setRole(r);
                return re;
            })
            .collect(Collectors.toSet());

        // update
        Set<SubjectAuthorityEntity> toDelete = oldRoles
            .stream()
            .filter(r -> !newRoles.contains(r))
            .collect(Collectors.toSet());
        Set<SubjectAuthorityEntity> toAdd = newRoles
            .stream()
            .filter(r -> !oldRoles.contains(r))
            .collect(Collectors.toSet());

        authorityRepository.deleteAll(toDelete);
        authorityRepository.saveAll(toAdd);

        return authorityRepository
            .findBySubjectAndRealm(uuid, realm)
            .stream()
            .map(a -> toAuthority(a))
            .collect(Collectors.toList());
    }

    public List<GrantedAuthority> updateAuthorities(String uuid, Collection<Map.Entry<String, String>> roles)
        throws NoSuchSubjectException {
        //fetch subject
        Subject s = findSubject(uuid);
        if (s == null) {
            throw new NoSuchSubjectException("subject not found for " + uuid);
        }

        // fetch current roles
        List<SubjectAuthorityEntity> oldRoles = authorityRepository.findBySubject(uuid);

        // unpack roles
        Set<SubjectAuthorityEntity> newRoles = roles
            .stream()
            .map(e -> {
                SubjectAuthorityEntity re = new SubjectAuthorityEntity(uuid);
                re.setRealm(e.getKey());
                re.setRole(e.getValue());
                return re;
            })
            .collect(Collectors.toSet());

        // update
        Set<SubjectAuthorityEntity> toDelete = oldRoles
            .stream()
            .filter(r -> !newRoles.contains(r))
            .collect(Collectors.toSet());
        Set<SubjectAuthorityEntity> toAdd = newRoles
            .stream()
            .filter(r -> !oldRoles.contains(r))
            .collect(Collectors.toSet());

        authorityRepository.deleteAll(toDelete);
        authorityRepository.saveAll(toAdd);

        return authorityRepository.findBySubject(uuid).stream().map(a -> toAuthority(a)).collect(Collectors.toList());
    }

    public void deleteAuthorities(String subjectId) {
        List<SubjectAuthorityEntity> roles = authorityRepository.findBySubject(subjectId);
        if (!roles.isEmpty()) {
            // remove
            authorityRepository.deleteAll(roles);
        }
    }

    public void deleteAuthorities(String subjectId, String realm) {
        List<SubjectAuthorityEntity> roles = authorityRepository.findBySubjectAndRealm(subjectId, realm);
        if (!roles.isEmpty()) {
            // remove
            authorityRepository.deleteAll(roles);
        }
    }

    private GrantedAuthority toAuthority(SubjectAuthorityEntity authority) {
        if (StringUtils.hasText(authority.getRealm())) {
            return new SimpleGrantedAuthority(authority.getRealm() + ":" + authority.getRole());
        } else {
            return new SimpleGrantedAuthority(authority.getRole());
        }
    }
}
