package it.smartcommunitylab.aac.core.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "subjects")
@EntityListeners(AuditingEntityListener.class)
public class SubjectEntity {

    @Id
    @Column(name = "subject_id", unique = true)
    private String subjectId;

    @NotNull
    private String realm;

    // base
    private String type;
    private String name;

    protected SubjectEntity() {

    }

    public SubjectEntity(String subjectId) {
        super();
        this.subjectId = subjectId;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
