package it.smartcommunitylab.aac.roles.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

@Entity
@Table(
    name = "space_roles",
    uniqueConstraints = @UniqueConstraint(columnNames = { "subject_id", "context", "space", "role" })
)
public class SpaceRoleEntity {

    @Id
    @GeneratedValue
    private Long id;

    // user
    @NotNull
    @Column(name = "subject_id", length = 128)
    private String subject;

    // role definition
    private String context;

    @NotNull
    @Column(length = 128)
    private String space;

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

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getSpace() {
        return space;
    }

    public void setSpace(String space) {
        this.space = space;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((context == null) ? 0 : context.hashCode());
        result = prime * result + ((role == null) ? 0 : role.hashCode());
        result = prime * result + ((space == null) ? 0 : space.hashCode());
        result = prime * result + ((subject == null) ? 0 : subject.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        SpaceRoleEntity other = (SpaceRoleEntity) obj;
        if (context == null) {
            if (other.context != null) return false;
        } else if (!context.equals(other.context)) return false;
        if (role == null) {
            if (other.role != null) return false;
        } else if (!role.equals(other.role)) return false;
        if (space == null) {
            if (other.space != null) return false;
        } else if (!space.equals(other.space)) return false;
        if (subject == null) {
            if (other.subject != null) return false;
        } else if (!subject.equals(other.subject)) return false;
        return true;
    }
}
