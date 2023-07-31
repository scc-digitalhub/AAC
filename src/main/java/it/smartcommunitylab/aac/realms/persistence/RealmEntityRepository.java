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

package it.smartcommunitylab.aac.realms.persistence;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RealmEntityRepository extends CustomJpaRepository<RealmEntity, String> {
    RealmEntity findBySlug(String slug);

    List<RealmEntity> findByIsPublic(boolean isPublic);

    List<RealmEntity> findBySlugContainingIgnoreCase(String keywords);
    List<RealmEntity> findByNameContainingIgnoreCase(String keywords);

    @Query(
        "select r from RealmEntity r where lower(r.slug) like lower(concat('%', ?1,'%')) or LOWER(r.name) like lower(concat('%', ?1,'%'))"
    )
    Page<RealmEntity> findByKeywords(String keywords, Pageable pageRequest);
}
