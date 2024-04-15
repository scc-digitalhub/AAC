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

package it.smartcommunitylab.aac.model;

import it.smartcommunitylab.aac.SystemKeys;
import java.io.Serializable;
import javax.validation.constraints.Size;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

//TODO remove authPrincipal from here and split to SubjectAuthenticatedPrincipal
public class Subject implements AuthenticatedPrincipal, Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    @Size(max = 128)
    private final String subjectId;

    @Size(max = 128)
    private final String realm;

    private final String name;

    private final String type;

    public Subject(String subject, String realm, String name, String type) {
        Assert.notNull(subject, "subject can not be null");
        this.subjectId = subject;
        this.realm = realm;
        this.type = type;
        if (StringUtils.hasText(name)) {
            this.name = name;
        } else {
            this.name = subject;
        }
    }

    @Override
    public String getName() {
        return name;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public String getRealm() {
        return realm;
    }

    public String getType() {
        return type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((subjectId == null) ? 0 : subjectId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Subject other = (Subject) obj;
        if (subjectId == null) {
            if (other.subjectId != null) return false;
        } else if (!subjectId.equals(other.subjectId)) return false;
        return true;
    }

    @Override
    public String toString() {
        return "Subject [subjectId=" + subjectId + ", realm=" + realm + ", name=" + name + "]";
    }
}
