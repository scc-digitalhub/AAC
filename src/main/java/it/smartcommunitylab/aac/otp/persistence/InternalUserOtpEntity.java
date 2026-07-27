package it.smartcommunitylab.aac.otp.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "internal_user_OTP", uniqueConstraints = @UniqueConstraint(columnNames = { "repository_id" }))
@EntityListeners(AuditingEntityListener.class)
public class InternalUserOtpEntity {

    @Id
    @NotBlank
    @Column(name = "id", length = 128)
    private String id;

    @NotNull
    @Column(name = "repository_id", length = 128)
    private String repositoryId;

    @NotNull
    @Column(name = "user_id", length = 128)
    private String userId;

    @NotNull
    @Column(length = 128)
    private String realm;

    @NotNull
    @Column(length = 512)
    private String token;

    @NotNull
    @Column(name = "expiry_timestamp")
    private long expiryTimestamp;

    @NotNull
    @Column(name = "attempts")
    private int attempts;

    @NotNull
    @Column(name = "consumed")
    private boolean consumed;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public long getExpiryTimestamp() {
        return expiryTimestamp;
    }

    public void setExpiryTimestamp(long expiryTimestamp) {
        this.expiryTimestamp = expiryTimestamp;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public boolean isConsumed() {
        return consumed;
    }

    public void setConsumed(boolean consumed) {
        this.consumed = consumed;
    }

    @Override
    public String toString() {
        return (
            "InternalUserOTPEntity [id=" +
            id +
            ", repositoryId=" +
            repositoryId +
            ", userId=" +
            userId +
            ", realm=" +
            realm +
            ", token=" +
            token +
            ", expiryTimestamp=" +
            expiryTimestamp +
            ", attempts=" +
            attempts +
            ", consumed=" +
            consumed +
            "]"
        );
    }
}
