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

    private int attempts;

    private boolean consumed;

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
    public void eraseCredentials() {
        this.token = null;
    }

    @Override
    public boolean isActive() {
        return !consumed;
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
    public String getStatus() {
        return consumed ? "consumed" : "active";
    }

    @Override
    public void setStatus(String status) {
        // Status management handled by consumed flag
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
            ", attempts=" +
            attempts +
            ", consumed=" +
            consumed +
            "]"
        );
    }
}
