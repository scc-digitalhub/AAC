package it.smartcommunitylab.aac.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.Collections;
import java.util.Set;
import javax.validation.constraints.NotBlank;
import org.springframework.security.core.GrantedAuthority;

@JsonInclude(Include.NON_NULL)
public class Developer {

    @NotBlank
    private String subjectId;

    private String realm;

    private String username;
    private String email;

    // authorities in AAC
    // these are either global or realm scoped
    private Set<GrantedAuthority> authorities;

    public Developer(String subjectId, String realm) {
        this.subjectId = subjectId;
        this.realm = realm;
        this.authorities = Collections.emptySet();
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(Set<GrantedAuthority> authorities) {
        this.authorities = authorities;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((authorities == null) ? 0 : authorities.hashCode());
        result = prime * result + ((email == null) ? 0 : email.hashCode());
        result = prime * result + ((realm == null) ? 0 : realm.hashCode());
        result = prime * result + ((subjectId == null) ? 0 : subjectId.hashCode());
        result = prime * result + ((username == null) ? 0 : username.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Developer other = (Developer) obj;
        if (authorities == null) {
            if (other.authorities != null) return false;
        } else if (!authorities.equals(other.authorities)) return false;
        if (email == null) {
            if (other.email != null) return false;
        } else if (!email.equals(other.email)) return false;
        if (realm == null) {
            if (other.realm != null) return false;
        } else if (!realm.equals(other.realm)) return false;
        if (subjectId == null) {
            if (other.subjectId != null) return false;
        } else if (!subjectId.equals(other.subjectId)) return false;
        if (username == null) {
            if (other.username != null) return false;
        } else if (!username.equals(other.username)) return false;
        return true;
    }
}
