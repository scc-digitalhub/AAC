package it.smartcommunitylab.aac.core.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "authorities", uniqueConstraints = @UniqueConstraint(columnNames = { "subject_id", "realm", "role" }))
public class SubjectAuthorityEntity {

    @Id
    @GeneratedValue
    private Long id;

    @NotNull
    @Column(name = "subject_id", length = 128)
    private String subject;

    @Column(length = 128)
    private String realm;

    @NotNull
    @Column(length = 128)
    private String role;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    protected SubjectAuthorityEntity() {}

    public SubjectAuthorityEntity(String subject) {
        super();
        this.subject = subject;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((realm == null) ? 0 : realm.hashCode());
        result = prime * result + ((role == null) ? 0 : role.hashCode());
        result = prime * result + ((subject == null) ? 0 : subject.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        SubjectAuthorityEntity other = (SubjectAuthorityEntity) obj;
        if (realm == null) {
            if (other.realm != null) return false;
        } else if (!realm.equals(other.realm)) return false;
        if (role == null) {
            if (other.role != null) return false;
        } else if (!role.equals(other.role)) return false;
        if (subject == null) {
            if (other.subject != null) return false;
        } else if (!subject.equals(other.subject)) return false;
        return true;
    }

    @Override
    public String toString() {
        return (
            "SubjectAuthorityEntity [id=" + id + ", subject=" + subject + ", realm=" + realm + ", role=" + role + "]"
        );
    }
}
