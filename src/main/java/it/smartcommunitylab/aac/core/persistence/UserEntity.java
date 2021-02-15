package it.smartcommunitylab.aac.core.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "users")
public class UserEntity {

    // TODO remove numeric id, we should have UUID to avoid locking on create
    @Id
    @GeneratedValue
    private Long id;

    @NotNull
    @Column(unique = true)
    private String uuid;

    private String username;

    protected UserEntity() {
    };

    public UserEntity(@NotNull String uuid) {
        super();
        this.uuid = uuid;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

}
