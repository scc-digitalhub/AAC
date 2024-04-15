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

package it.smartcommunitylab.aac.saml.persistence;

import it.smartcommunitylab.aac.SystemKeys;
import java.io.Serializable;

public class SamlUserAccountId implements Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_SAML_SERIAL_VERSION;

    private String repositoryId;
    private String subjectId;

    public SamlUserAccountId() {}

    public SamlUserAccountId(String repositoryId, String subjectId) {
        super();
        this.repositoryId = repositoryId;
        this.subjectId = subjectId;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((repositoryId == null) ? 0 : repositoryId.hashCode());
        result = prime * result + ((subjectId == null) ? 0 : subjectId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        SamlUserAccountId other = (SamlUserAccountId) obj;
        if (repositoryId == null) {
            if (other.repositoryId != null) return false;
        } else if (!repositoryId.equals(other.repositoryId)) return false;
        if (subjectId == null) {
            if (other.subjectId != null) return false;
        } else if (!subjectId.equals(other.subjectId)) return false;
        return true;
    }
}
