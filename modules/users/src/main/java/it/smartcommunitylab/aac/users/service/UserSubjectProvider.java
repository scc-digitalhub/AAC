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

package it.smartcommunitylab.aac.users.service;

import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.core.provider.SubjectProvider;
import it.smartcommunitylab.aac.model.UserStatus;
import it.smartcommunitylab.aac.users.model.UserSubject;
import it.smartcommunitylab.aac.users.persistence.UserEntity;
import it.smartcommunitylab.aac.users.persistence.UserEntityRepository;
import java.util.Collection;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Service
@Slf4j
@Transactional
public class UserSubjectProvider implements SubjectProvider<UserSubject> {

    private final UserEntityRepository repository;

    private Converter<UserEntity, UserSubject> converter = e -> {
        return UserSubject.builder()
            .userId(e.getUuid())
            .realm(e.getRealm())
            .userName(e.getUsername())
            .status(e.getStatus() != null ? UserStatus.parse(e.getStatus()) : UserStatus.ACTIVE)
            .build();
    };

    public UserSubjectProvider(UserEntityRepository userRepository) {
        Assert.notNull(userRepository, "user repository is mandatory");

        this.repository = userRepository;
    }

    public void setConverter(Converter<UserEntity, UserSubject> converter) {
        Assert.notNull(converter, "converter can not be null");
        this.converter = converter;
    }

    @Override
    public Collection<UserSubject> listSubjects() {
        log.debug("list all subjects");

        return repository.findAll().stream().map(e -> converter.convert(e)).collect(Collectors.toList());
    }

    @Override
    public Collection<UserSubject> listSubjectsByRealm(String realm) {
        log.debug("list subjects for realm {}", String.valueOf(realm));

        return repository.findByRealm(realm).stream().map(e -> converter.convert(e)).collect(Collectors.toList());
    }

    @Override
    public Page<UserSubject> listSubjectsByRealm(String realm, Pageable page) {
        log.debug("list subjects for realm {} - page {}", String.valueOf(realm), String.valueOf(page));

        return repository.findByRealm(realm, page).map(e -> converter.convert(e));
    }

    @Override
    public UserSubject findSubject(String id) {
        log.debug("find subject with id {}", String.valueOf(id));

        UserEntity e = repository.findOne(id);
        if (e == null) {
            return null;
        }

        return converter.convert(e);
    }

    @Override
    public UserSubject getSubject(String id) throws NoSuchSubjectException {
        log.debug("get subject with id {}", String.valueOf(id));

        UserEntity e = repository.findOne(id);
        if (e == null) {
            throw new NoSuchSubjectException();
        }

        return converter.convert(e);
    }

    @Override
    public Page<UserSubject> searchSubjectsByRealm(String realm, String q, Pageable page) {
        log.debug("search subjects for realm {} via q: {}", String.valueOf(realm), String.valueOf(q));

        return repository
            .findByRealmAndUsernameContainingIgnoreCaseOrRealmAndUuidContainingIgnoreCaseOrRealmAndEmailAddressContainingIgnoreCase(
                realm,
                q,
                realm,
                q,
                realm,
                q,
                page
            )
            .map(e -> converter.convert(e));
    }
}
