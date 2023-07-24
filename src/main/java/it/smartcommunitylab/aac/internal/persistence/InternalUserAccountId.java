package it.smartcommunitylab.aac.internal.persistence;

import it.smartcommunitylab.aac.SystemKeys;
import java.io.Serializable;

public class InternalUserAccountId implements Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private String repositoryId;
    private String username;

    public InternalUserAccountId() {}

    public InternalUserAccountId(String provider, String username) {
        super();
        this.repositoryId = provider;
        this.username = username;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((repositoryId == null) ? 0 : repositoryId.hashCode());
        result = prime * result + ((username == null) ? 0 : username.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        InternalUserAccountId other = (InternalUserAccountId) obj;
        if (repositoryId == null) {
            if (other.repositoryId != null) return false;
        } else if (!repositoryId.equals(other.repositoryId)) return false;
        if (username == null) {
            if (other.username != null) return false;
        } else if (!username.equals(other.username)) return false;
        return true;
    }
}
