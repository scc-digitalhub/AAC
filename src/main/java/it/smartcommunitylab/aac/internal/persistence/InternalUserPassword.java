package it.smartcommunitylab.aac.internal.persistence;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotBlank;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.CredentialsContainer;

import com.fasterxml.jackson.annotation.JsonIgnore;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractUserCredentials;
import it.smartcommunitylab.aac.internal.model.CredentialsStatus;
import it.smartcommunitylab.aac.internal.model.CredentialsType;

@Entity
@Table(name = "internal_users_passwords", uniqueConstraints = @UniqueConstraint(columnNames = {
        "provider_id",
        "reset_key" }))

@EntityListeners(AuditingEntityListener.class)
public class InternalUserPassword extends AbstractUserCredentials
        implements CredentialsContainer, Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    @Id
    private String id;

    // account id
    @NotBlank
    @Column(name = "provider_id", length = 128)
    private String provider;

    @NotBlank
    @Column(name = "username", length = 128)
    private String username;

    @NotBlank
    private String password;

    private String status;

    @CreatedDate
    @Column(name = "created_date")
    private Date createDate;

    @Column(name = "expiration_date")
    private Date expirationDate;

    @Column(name = "reset_deadline")
    private Date resetDeadline;

    @Column(name = "reset_key", nullable = true)
    private String resetKey;

    @Column(name = "change_first_access")
    private Boolean changeOnFirstAccess;

    private transient String realm;

    public InternalUserPassword() {
        super(SystemKeys.AUTHORITY_INTERNAL, null, null, null);
    }

    @Override
    public String getProvider() {
        return provider;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public CredentialsType getCredentialsType() {
        return CredentialsType.PASSWORD;
    }

    @Override
    public String getUuid() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    @JsonIgnore
    public String getCredentials() {
        return password;
    }

    @Override
    public boolean isActive() {
        return CredentialsStatus.ACTIVE.getValue().equals(status);
    }

    public boolean isExpired() {
        return expirationDate == null ? false : expirationDate.before(new Date());
    }

    @Override
    public boolean isRevoked() {
        return CredentialsStatus.REVOKED.getValue().equals(status);
    }

    @Override
    public boolean isChangeOnFirstAccess() {
        return changeOnFirstAccess != null ? changeOnFirstAccess.booleanValue() : false;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Date getResetDeadline() {
        return resetDeadline;
    }

    public void setResetDeadline(Date resetDeadline) {
        this.resetDeadline = resetDeadline;
    }

    public String getResetKey() {
        return resetKey;
    }

    public void setResetKey(String resetKey) {
        this.resetKey = resetKey;
    }

    public Boolean getChangeOnFirstAccess() {
        return changeOnFirstAccess;
    }

    public void setChangeOnFirstAccess(Boolean changeOnFirstAccess) {
        this.changeOnFirstAccess = changeOnFirstAccess;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    @Override
    public void eraseCredentials() {
        this.password = null;
        this.resetKey = null;
    }

}
