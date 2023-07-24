package it.smartcommunitylab.aac.openid.persistence;

import it.smartcommunitylab.aac.SystemKeys;
import java.io.Serializable;

public class OIDCUserAccountId implements Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_OIDC_SERIAL_VERSION;

    private String repositoryId;
    private String subject;

    public OIDCUserAccountId() {}

    public OIDCUserAccountId(String repositoryId, String userId) {
        super();
        this.repositoryId = repositoryId;
        this.subject = userId;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((repositoryId == null) ? 0 : repositoryId.hashCode());
        result = prime * result + ((subject == null) ? 0 : subject.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        OIDCUserAccountId other = (OIDCUserAccountId) obj;
        if (repositoryId == null) {
            if (other.repositoryId != null) return false;
        } else if (!repositoryId.equals(other.repositoryId)) return false;
        if (subject == null) {
            if (other.subject != null) return false;
        } else if (!subject.equals(other.subject)) return false;
        return true;
    }
}
