package it.smartcommunitylab.aac.model;

import java.io.Serializable;

import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;

//TODO remove authPrincipal from here and split to SubjectAuthenticatedPrincipal
public class Subject implements AuthenticatedPrincipal, Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    private final String subjectId;
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
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Subject other = (Subject) obj;
        if (subjectId == null) {
            if (other.subjectId != null)
                return false;
        } else if (!subjectId.equals(other.subjectId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Subject [subjectId=" + subjectId + ", realm=" + realm + ", name=" + name + "]";
    }

}
