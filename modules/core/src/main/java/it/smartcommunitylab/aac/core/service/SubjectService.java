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
import it.smartcommunitylab.aac.core.provider.SubjectProvider;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.repository.PaginationUtils.PageOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Service
@Slf4j
@Transactional
public class SubjectService {

    private TreeMap<String, SubjectProvider<? extends Subject>> providers;

    public SubjectService(List<SubjectProvider<? extends Subject>> providers) {
        Assert.notNull(providers, "providers can not be null");

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
}
