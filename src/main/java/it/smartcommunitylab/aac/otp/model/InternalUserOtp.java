package it.smartcommunitylab.aac.otp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.credentials.base.AbstractUserCredentials;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalUserOtp extends AbstractUserCredentials {

    public InternalUserOtp(String realm, String id) {
        super(SystemKeys.AUTHORITY_OTP, null, realm, id);
    }

    @SuppressWarnings("unused")
    protected InternalUserOtp() {
        super();
    }

    @NotBlank
    private String repositoryId;

    @NotBlank
    private String token;
    
    @NotBlank
    private Long expiryTimestamp;

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getRepositoryId() {
        return repositoryId;
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

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return (
            "InternalUserOtp [repositoryId=" +
            repositoryId +
            ", token=" +
            token +
            ", expiryTimestamp=" +
            expiryTimestamp +
            "]"
        );
    }

    @Override
    public void eraseCredentials() {
        throw new UnsupportedOperationException("Unimplemented method 'getCredentials'");
    }

    @Override
    public String getCredentials() {
        throw new UnsupportedOperationException("Unimplemented method 'getCredentials'");
    }

    @Override
    public String getUuid() {
        throw new UnsupportedOperationException("Unimplemented method 'getUuid'");
    }

    public static Long getSerialversionuid() {
        throw new UnsupportedOperationException("Unimplemented method 'getSerialversionuid'");
    }

    public static String getResourceType() {
        throw new UnsupportedOperationException("Unimplemented method 'getResourceType'");
    }

    @Override
    public boolean isRevoked() {
        throw new UnsupportedOperationException("Unimplemented method 'isRevoked'");
    }

    @Override
    public boolean isExpired() {
        throw new UnsupportedOperationException("Unimplemented method 'isExpired'");
    }

    @Override
    public void setStatus(String status) {
        throw new UnsupportedOperationException("Unimplemented method 'setStatus'");
    }

    @Override
    public boolean isActive() {
        throw new UnsupportedOperationException("Unimplemented method 'isActive'");
    }

    @Override
    public String getStatus() {
        throw new UnsupportedOperationException("Unimplemented method 'getStatus'");
    }
}
