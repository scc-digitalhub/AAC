package it.smartcommunitylab.aac.internal.persistence;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "internal_attributes", uniqueConstraints = @UniqueConstraint(columnNames = { "provider",
        "subject_id", "attribute_set", "attr_key" }))
public class InternalAttributeEntity {

    @Id
    @GeneratedValue
    private Long id;

    @NotNull
    private String provider;

    @NotNull
    @Column(name = "subject_id")
    private String subjectId;

    @NotNull
    @Column(name = "attribute_set")
    private String set;

    @Column(name = "attr_key")
    private String key;

    @Column(name = "attr_type")
    private String type;

    @Lob
    @Column(name = "attr_value")
    private Serializable value;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getSet() {
        return set;
    }

    public void setSet(String set) {
        this.set = set;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Serializable getValue() {
        return value;
    }

    public void setValue(Serializable value) {
        this.value = value;
    }

}
