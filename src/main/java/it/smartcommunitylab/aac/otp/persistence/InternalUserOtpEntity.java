package it.smartcommunitylab.aac.otp.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "internal_user_OTP")
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
    @Column(name = "provider_id", length = 128)
    private String providerId;

    @NotNull
    @Column(length = 6)
    private String token;

    @NotNull
    @Column(name = "expiry_timestamp", nullable = false)
    private Long expiryTimestamp;

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

    public Long getExpiryTimestamp() {
        return expiryTimestamp;
    }

    public void setExpiryTimestamp(Long expiryTimestamp) {
        this.expiryTimestamp = expiryTimestamp;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
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
            "]"
        );
    }
}
