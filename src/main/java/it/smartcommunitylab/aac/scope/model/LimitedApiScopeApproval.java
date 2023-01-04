package it.smartcommunitylab.aac.scope.model;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonIgnore;

import it.smartcommunitylab.aac.scope.base.AbstractScopeApproval;

public class LimitedApiScopeApproval extends AbstractScopeApproval {

    private ApprovalStatus status;
    private String id;
    private Date expiresAt;
    private Date createdAt;

    public LimitedApiScopeApproval(String resourceId, String scope, String subject, String client, int expiresIn) {
        super(resourceId, scope, subject, client);

        // generate a random unique id, this is not persisted but transient
        this.id = UUID.randomUUID().toString();

        this.createdAt = new Date();

        if (expiresIn > 0) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.SECOND, expiresIn);
            expiresAt = cal.getTime();
        }
    }

    public LimitedApiScopeApproval(String resourceId, String scope, String subject, String client, int expiresIn,
            ApprovalStatus status) {
        this(resourceId, scope, subject, client, expiresIn);
        Assert.notNull(status, "status can not be null");
        this.status = status;
    }

    public void setStatus(ApprovalStatus status) {
        this.status = status;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public ApprovalStatus getStatus() {
        return status;
    }

    @Override
    public String getId() {
        return id;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    @JsonIgnore
    public Approval getApproval() {
        if (status == null) {
            return null;
        }

        Approval.ApprovalStatus as = ApprovalStatus.APPROVED == status ? Approval.ApprovalStatus.APPROVED
                : Approval.ApprovalStatus.DENIED;
        return new Approval(subject, client, scope, expiresAt, as, createdAt);
    }

    @Override
    public boolean isApproved() {
        return ApprovalStatus.APPROVED == status ? true : false;
    }

    @Override
    public long expiresIn() {
        if (expiresAt == null) {
            return -1;
        }

        Date now = new Date();
        return (expiresAt.getTime() - now.getTime()) / 1000L;

    }

}
