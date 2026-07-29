package it.smartcommunitylab.aac.otp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.credentials.base.AbstractUserCredentials;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalUserOtp extends AbstractUserCredentials {

    private static final Long serialVersionUID = SystemKeys.AAC_INTERNAL_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_CREDENTIALS + SystemKeys.ID_SEPARATOR + SystemKeys.AUTHORITY_OTP;

    @NotBlank
    private String repositoryId;

    @NotBlank
    private String token;

    private Long expiryTimestamp;

    public InternalUserOtp(String realm, String id) {
        super(SystemKeys.AUTHORITY_OTP, null, realm, id);
    }

    @SuppressWarnings("unused")
    protected InternalUserOtp() {
        super();
    }

    public static Long getSerialversionuid() {
        return serialVersionUID;
    }

    public static String getResourceType() {
        return RESOURCE_TYPE;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
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
    public void eraseCredentials() {
        this.token = null;
    }

    @Override
    public boolean isRevoked() {
        return false;
    }

    @Override
    public boolean isExpired() {
        return expiryTimestamp != null && expiryTimestamp < System.currentTimeMillis();
    }

    @Override
    public String getCredentials() {
        return token;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return super.getUserId();
    }

    @Override
    public void setStatus(String status) {
        // Status management handled by deletion
    }

    @Override
    public boolean isActive() {
        throw new UnsupportedOperationException("Unimplemented method 'isActive'");
    }

    @Override
    public String getStatus() {
        throw new UnsupportedOperationException("Unimplemented method 'getStatus'");
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
}
