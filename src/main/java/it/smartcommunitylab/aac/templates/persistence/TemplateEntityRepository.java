/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.templates.persistence;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import it.smartcommunitylab.aac.repository.DetachableJpaRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface TemplateEntityRepository
    extends CustomJpaRepository<TemplateEntity, String>, DetachableJpaRepository<TemplateEntity> {
    TemplateEntity findByAuthorityAndRealmAndTemplateAndLanguage(
        String authority,
        String realm,
        String template,
        String language
    );

    List<TemplateEntity> findByRealm(String realm);

    List<TemplateEntity> findByAuthority(String authority);

    List<TemplateEntity> findByAuthorityAndRealm(String authority, String realm);

    List<TemplateEntity> findByAuthorityAndRealmAndTemplate(String authority, String realm, String template);

    Page<TemplateEntity> findByRealm(String realm, Pageable page);

    Page<TemplateEntity> findByRealmAndAuthorityContainingIgnoreCaseOrRealmAndTemplateContainingIgnoreCase(
        String realma,
        String authority,
        String realmt,
        String template,
        Pageable page
    );
}
