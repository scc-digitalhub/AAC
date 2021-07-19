package it.smartcommunitylab.aac.core.auth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;

public class RealmGrantedAuthority implements GrantedAuthority {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final String realm;
    private final String role;

    public RealmGrantedAuthority(String realm, String role) {
        Assert.hasText(realm, "A space textual representation is required");
        Assert.hasText(role, "A granted authority textual representation is required");
        this.realm = realm;
        this.role = role;
    }

    public String getRealm() {
        return realm;
    }

    public String getRole() {
        return role;
    }

    @Override
    public String getAuthority() {
        return realm + ":" + role;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((role == null) ? 0 : role.hashCode());
        result = prime * result + ((realm == null) ? 0 : realm.hashCode());
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
        RealmGrantedAuthority other = (RealmGrantedAuthority) obj;
        if (role == null) {
            if (other.role != null)
                return false;
        } else if (!role.equals(other.role))
            return false;
        if (realm == null) {
            if (other.realm != null)
                return false;
        } else if (!realm.equals(other.realm))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return getAuthority();
    }

}
