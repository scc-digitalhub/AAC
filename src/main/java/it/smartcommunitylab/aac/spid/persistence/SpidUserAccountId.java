package it.smartcommunitylab.aac.spid.persistence;

import java.io.Serializable;

import it.smartcommunitylab.aac.SystemKeys;

public class SpidUserAccountId implements Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_SPID_SERIAL_VERSION;

    private String provider;
    private String subjectId;

    public SpidUserAccountId() {
    }

    public SpidUserAccountId(String provider, String subjectId) {
        super();
        this.provider = provider;
        this.subjectId = subjectId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
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
        result = prime * result + ((provider == null) ? 0 : provider.hashCode());
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
        SpidUserAccountId other = (SpidUserAccountId) obj;
        if (provider == null) {
            if (other.provider != null)
                return false;
        } else if (!provider.equals(other.provider))
            return false;
        if (subjectId == null) {
            if (other.subjectId != null)
                return false;
        } else if (!subjectId.equals(other.subjectId))
            return false;
        return true;
    }

}