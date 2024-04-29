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

package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.common.NoSuchSubjectException;
import it.smartcommunitylab.aac.model.Subject;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/*
 * A provider for subjects of a given type
 */
public interface SubjectProvider<S extends Subject> {
    /*
     * Subjects managed by this provider
     */
    public Collection<S> listSubjects();

    public Collection<S> listSubjectsByRealm(String realm);

    public Page<S> listSubjectsByRealm(String realm, Pageable page);

    public Page<S> searchSubjectsByRealm(String realm, String q, Pageable page);

    public S findSubject(String id);

    public S getSubject(String id) throws NoSuchSubjectException;
}
